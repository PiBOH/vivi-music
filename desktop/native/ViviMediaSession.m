// ViviMediaSession.m — macOS system "Now Playing" integration for VIVI Music DE.
//
// Registers the app with the system media session (Control Center / Lock Screen
// "Now Playing" tile) via MediaPlayer.framework and answers the physical media
// keys (Play/Pause, Next, Previous — keyboard, Touch Bar, headset buttons)
// through MPRemoteCommandCenter.
//
// Compiled per-architecture by the CI workflow into a dylib that is shipped as
// a classpath resource and loaded at runtime through JNA (see MacMediaSession.kt).
// The bundle identifier set by jpackage is not a registered media app, so the
// bundle's display name is used as the now-playing app name.
//
// All MediaPlayer state must only be touched from the main thread.

#import <MediaPlayer/MediaPlayer.h>
#import <AppKit/AppKit.h>
#import <Foundation/Foundation.h>
#import <UserNotifications/UserNotifications.h>

// ---------------------------------------------------------------------------
// JNA callbacks (registered by Kotlin before the session starts).
// ---------------------------------------------------------------------------
typedef void (*vivi_play_pause_cb)(void);
typedef void (*vivi_next_cb)(void);
typedef void (*vivi_previous_cb)(void);
typedef void (*vivi_seek_cb)(double positionMs);
typedef void (*vivi_artwork_cb)(void);

static vivi_play_pause_cb g_playPause = NULL;
static vivi_next_cb g_next = NULL;
static vivi_previous_cb g_previous = NULL;
static vivi_seek_cb g_seek = NULL;
static vivi_artwork_cb g_artwork = NULL;

void viviRegisterSeekCallback(vivi_seek_cb cb);   // defined below
void viviDispatchSeek(double positionMs);         // defined below

// Current now-playing state (kept so a late metadata push re-applies it).
static NSString *g_title = nil;
static NSString *g_artist = nil;
static NSString *g_album = nil;
static BOOL g_playing = NO;
static NSTimeInterval g_duration = 0.0;
static NSTimeInterval g_position = 0.0;
static NSString *g_artworkPath = nil; // local file already downloaded by Kotlin

// ---------------------------------------------------------------------------
// Notification delegate: shows banners even while the app is in the foreground.
// Kept strongly referenced for the app's lifetime.
// ---------------------------------------------------------------------------
@interface ViviNotificationDelegate : NSObject <UNUserNotificationCenterDelegate>
@end

@implementation ViviNotificationDelegate
- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler {
    completionHandler(UNNotificationPresentationOptionBanner |
                      UNNotificationPresentationOptionList |
                      UNNotificationPresentationOptionSound);
}
@end

static ViviNotificationDelegate *g_notificationDelegate = nil;

static NSImage *LoadImageSafely(NSString *path) {
    if (path.length == 0) return nil;
    NSData *data = [NSData dataWithContentsOfFile:path];
    if (data.length == 0) return nil;
    // ImageIO decodes without touching AppKit's uncached image machinery, so
    // invalid/tiny files never crash or log AppKit warnings.
    CGImageSourceRef src = CGImageSourceCreateWithData((__bridge CFDataRef)data, NULL);
    if (!src) return nil;
    CGImageRef cg = CGImageSourceCreateImageAtIndex(src, 0, NULL);
    CFRelease(src);
    if (!cg) return nil;
    NSImage *img = [[NSImage alloc] initWithCGImage:cg size:NSZeroSize];
    CGImageRelease(cg);
    return img;
}

static void PushNowPlayingInfo(void) {
    NSMutableDictionary *info = [NSMutableDictionary dictionary];
    if (g_title.length > 0) info[MPMediaItemPropertyTitle] = g_title;
    if (g_artist.length > 0) info[MPMediaItemPropertyArtist] = g_artist;
    if (g_album.length > 0) info[MPMediaItemPropertyAlbumTitle] = g_album;
    if (g_duration > 0.0) info[MPMediaItemPropertyPlaybackDuration] = @(g_duration);
    // The real position (0 while paused) keeps the Lock Screen / Control Center
    // scrubber slider state visible and correct.
    info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = @(MAX(g_position, 0.0));
    info[MPNowPlayingInfoPropertyPlaybackRate] = g_playing ? @1.0 : @0.0;
    NSImage *art = LoadImageSafely(g_artworkPath);
    if (art) {
        info[MPMediaItemPropertyArtwork] =
            [[MPMediaItemArtwork alloc] initWithBoundsSize:art.size
                                           requestHandler:^NSImage *(CGSize size) {
                                               return art;
                                           }];
    }
    [MPNowPlayingInfoCenter defaultCenter].nowPlayingInfo = info;
}

// ---------------------------------------------------------------------------
// Exported C API (loaded by JNA).
// ---------------------------------------------------------------------------

// Registers the Kotlin callbacks and installs the remote-command handlers.
// Call once, before any metadata is pushed.
void viviRegisterCallbacks(vivi_play_pause_cb pp, vivi_next_cb nx, vivi_previous_cb pv,
                           vivi_seek_cb sk, vivi_artwork_cb art) {
    g_playPause = pp;
    g_next = nx;
    g_previous = pv;
    g_seek = sk;
    g_artwork = art;

    dispatch_async(dispatch_get_main_queue(), ^{
        MPRemoteCommandCenter *center = [MPRemoteCommandCenter sharedCommandCenter];
        center.playCommand.enabled = YES;
        center.pauseCommand.enabled = YES;
        center.togglePlayPauseCommand.enabled = YES;
        center.nextTrackCommand.enabled = YES;
        center.previousTrackCommand.enabled = YES;
        center.changePlaybackPositionCommand.enabled = YES;

        [center.playCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
            if (g_playPause) g_playPause();
            return MPRemoteCommandHandlerStatusSuccess;
        }];
        [center.pauseCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
            if (g_playPause) g_playPause();
            return MPRemoteCommandHandlerStatusSuccess;
        }];
        [center.togglePlayPauseCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
            if (g_playPause) g_playPause();
            return MPRemoteCommandHandlerStatusSuccess;
        }];
        [center.nextTrackCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
            if (g_next) g_next();
            return MPRemoteCommandHandlerStatusSuccess;
        }];
        [center.previousTrackCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
            if (g_previous) g_previous();
            return MPRemoteCommandHandlerStatusSuccess;
        }];
        // Scrubbing from the Lock Screen / Control Center slider.
        [center.changePlaybackPositionCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
            MPChangePlaybackPositionCommandEvent *posEvent =
                (MPChangePlaybackPositionCommandEvent *)event;
            viviDispatchSeek(posEvent.positionTime * 1000.0);
            return MPRemoteCommandHandlerStatusSuccess;
        }];
    });
}

// Sets the app identity shown in the system tile.
void viviSetAppIdentity(const char *appNameUtf8) {
    NSString *name = appNameUtf8
                         ? [NSString stringWithUTF8String:appNameUtf8]
                         : @"VIVI Music";
    dispatch_async(dispatch_get_main_queue(), ^{
        g_title = name;
        PushNowPlayingInfo();
    });
}

// Enables the remote commands and announces the session. Idempotent — call it
// whenever playback starts so the tile appears even if it was cleared by
// viviEndSession earlier.
void viviStartSession(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        MPRemoteCommandCenter *center = [MPRemoteCommandCenter sharedCommandCenter];
        center.playCommand.enabled = YES;
        center.pauseCommand.enabled = YES;
        center.togglePlayPauseCommand.enabled = YES;
        center.nextTrackCommand.enabled = YES;
        center.previousTrackCommand.enabled = YES;
        center.changePlaybackPositionCommand.enabled = YES;
        PushNowPlayingInfo();
    });
}

// Updates the now-playing metadata. thumbnailPathUtf8 is a local file path
// (Kotlin downloads the artwork first) or NULL/empty to keep the previous one.
void viviSetNowPlaying(const char *titleUtf8, const char *artistUtf8, const char *albumUtf8,
                       double durationMs, double positionMs, int playing,
                       const char *thumbnailPathUtf8) {
    dispatch_async(dispatch_get_main_queue(), ^{
        g_title = titleUtf8 ? [NSString stringWithUTF8String:titleUtf8] : @"";
        g_artist = artistUtf8 ? [NSString stringWithUTF8String:artistUtf8] : @"";
        g_album = albumUtf8 ? [NSString stringWithUTF8String:albumUtf8] : @"";
        g_duration = durationMs / 1000.0;
        g_position = positionMs / 1000.0;
        g_playing = playing != 0;
        if (thumbnailPathUtf8 && strlen(thumbnailPathUtf8) > 0) {
            g_artworkPath = [NSString stringWithUTF8String:thumbnailPathUtf8];
        }
        PushNowPlayingInfo();
        if (g_artwork) g_artwork();
    });
}

// Marks the session as stopped (clears the tile; handlers stay registered).
void viviEndSession(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        [MPNowPlayingInfoCenter defaultCenter].nowPlayingInfo = nil;
    });
}

// Requests macOS notification permission (shown once by the system). Safe to
// call repeatedly; the OS ignores repeated prompts.
void viviRequestNotificationPermission(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!g_notificationDelegate) {
            g_notificationDelegate = [[ViviNotificationDelegate alloc] init];
        }
        [UNUserNotificationCenter currentNotificationCenter].delegate = g_notificationDelegate;
        [[UNUserNotificationCenter currentNotificationCenter]
            requestAuthorizationWithOptions:(UNAuthorizationOptionAlert |
                                             UNAuthorizationOptionSound |
                                             UNAuthorizationOptionBadge)
                          completionHandler:^(BOOL granted, NSError *error) {
            // Best-effort: macOS only shows the prompt once, so a denial is
            // permanent unless the user enables it in System Settings.
        }];
    });
}

// Posts a native Notification Center banner attributed to the app. Returns
// immediately; delivery depends on the permission granted earlier.
void viviNotify(const char *titleUtf8, const char *messageUtf8) {
    NSString *title = titleUtf8 ? [NSString stringWithUTF8String:titleUtf8] : @"";
    NSString *message = messageUtf8 ? [NSString stringWithUTF8String:messageUtf8] : @"";
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!g_notificationDelegate) {
            g_notificationDelegate = [[ViviNotificationDelegate alloc] init];
            [UNUserNotificationCenter currentNotificationCenter].delegate = g_notificationDelegate;
        }
        UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
        content.title = title;
        content.body = message;
        content.sound = [UNNotificationSound defaultSound];
        UNTimeIntervalNotificationTrigger *trigger =
            [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:0.1 repeats:NO];
        UNNotificationRequest *request =
            [UNNotificationRequest requestWithIdentifier:[NSUUID UUID].UUIDString
                                                withContent:content
                                                withTrigger:trigger];
        [[UNUserNotificationCenter currentNotificationCenter]
            addNotificationRequest:request withCompletionHandler:nil];
    });
}

// ---------------------------------------------------------------------------
// Seek trampoline (used by the scrubber handler above).
// ---------------------------------------------------------------------------
void viviRegisterSeekCallback(vivi_seek_cb cb) {
    g_seek = cb;
}

void viviDispatchSeek(double positionMs) {
    if (g_seek) {
        dispatch_async(dispatch_get_main_queue(), ^{
            g_seek(positionMs);
        });
    }
}
