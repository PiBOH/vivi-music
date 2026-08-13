-- Vivi Music DE - Supabase schema (initial migration)
-- Run this file once in the Supabase SQL editor (or via `supabase db push`).

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------------
-- Playlists
-- ---------------------------------------------------------------------------
create table if not exists public.playlists (
    id text primary key,
    user_id uuid not null,
    name text not null,
    description text,
    thumbnail_url text,
    created_at bigint not null default 0,
    updated_at bigint not null default 0
);

-- ---------------------------------------------------------------------------
-- Playlist songs (denormalized track metadata so rows are self-contained)
-- ---------------------------------------------------------------------------
create table if not exists public.playlist_songs (
    id text primary key,
    user_id uuid not null,
    playlist_id text not null,
    song_id text not null,
    title text not null default '',
    artist text not null default '',
    album text not null default '',
    thumbnail_url text,
    duration_ms bigint,
    position integer not null default 0,
    updated_at bigint not null default 0
);

-- ---------------------------------------------------------------------------
-- Favorites
-- ---------------------------------------------------------------------------
create table if not exists public.favorites (
    id text primary key,
    user_id uuid not null,
    song_id text not null,
    title text not null default '',
    artist text not null default '',
    album text not null default '',
    thumbnail_url text,
    duration_ms bigint,
    added_at bigint not null default 0
);

-- ---------------------------------------------------------------------------
-- History
-- ---------------------------------------------------------------------------
create table if not exists public.history (
    id text primary key,
    user_id uuid not null,
    song_id text not null,
    title text not null default '',
    artist text not null default '',
    album text not null default '',
    thumbnail_url text,
    duration_ms bigint,
    played_at bigint not null default 0
);

create index if not exists playlists_user_idx on public.playlists (user_id, updated_at);
create index if not exists playlist_songs_user_idx on public.playlist_songs (user_id, playlist_id);
create index if not exists favorites_user_idx on public.favorites (user_id, added_at);
create index if not exists history_user_idx on public.history (user_id, played_at);

-- ---------------------------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------------------------
alter table public.playlists enable row level security;
alter table public.playlist_songs enable row level security;
alter table public.favorites enable row level security;
alter table public.history enable row level security;

drop policy if exists "playlists_select_own" on public.playlists;
create policy "playlists_select_own" on public.playlists
    for select using (auth.uid() = user_id);
create policy "playlists_insert_own" on public.playlists
    for insert with check (auth.uid() = user_id);
create policy "playlists_update_own" on public.playlists
    for update using (auth.uid() = user_id);
create policy "playlists_delete_own" on public.playlists
    for delete using (auth.uid() = user_id);

drop policy if exists "playlist_songs_select_own" on public.playlist_songs;
create policy "playlist_songs_select_own" on public.playlist_songs
    for select using (auth.uid() = user_id);
create policy "playlist_songs_insert_own" on public.playlist_songs
    for insert with check (auth.uid() = user_id);
create policy "playlist_songs_update_own" on public.playlist_songs
    for update using (auth.uid() = user_id);
create policy "playlist_songs_delete_own" on public.playlist_songs
    for delete using (auth.uid() = user_id);

drop policy if exists "favorites_select_own" on public.favorites;
create policy "favorites_select_own" on public.favorites
    for select using (auth.uid() = user_id);
create policy "favorites_insert_own" on public.favorites
    for insert with check (auth.uid() = user_id);
create policy "favorites_update_own" on public.favorites
    for update using (auth.uid() = user_id);
create policy "favorites_delete_own" on public.favorites
    for delete using (auth.uid() = user_id);

drop policy if exists "history_select_own" on public.history;
create policy "history_select_own" on public.history
    for select using (auth.uid() = user_id);
create policy "history_insert_own" on public.history
    for insert with check (auth.uid() = user_id);
create policy "history_update_own" on public.history
    for update using (auth.uid() = user_id);
create policy "history_delete_own" on public.history
    for delete using (auth.uid() = user_id);

-- ---------------------------------------------------------------------------
-- Grants
-- ---------------------------------------------------------------------------
grant select, insert, update, delete on public.playlists to authenticated;
grant select, insert, update, delete on public.playlist_songs to authenticated;
grant select, insert, update, delete on public.favorites to authenticated;
grant select, insert, update, delete on public.history to authenticated;

-- ---------------------------------------------------------------------------
-- Realtime
-- ---------------------------------------------------------------------------
alter table public.playlists replica identity full;
alter table public.playlist_songs replica identity full;
alter table public.favorites replica identity full;
alter table public.history replica identity full;

alter publication supabase_realtime add table public.playlists;
alter publication supabase_realtime add table public.playlist_songs;
alter publication supabase_realtime add table public.favorites;
alter publication supabase_realtime add table public.history;
