-- NewsHub Android: align Supabase schema + RLS with app BuildConfig
-- Run in Supabase SQL Editor (project: szgqeryffmfhwbogybuv)

-- ---------------------------------------------------------------------------
-- Profile table (app: SUPABASE_PROFILE_TABLE=users)
-- ---------------------------------------------------------------------------
create table if not exists public.users (
  id uuid primary key default gen_random_uuid(),
  auth_user_id uuid not null unique references auth.users (id) on delete cascade,
  email text,
  first_name text not null default '',
  last_name text not null default '',
  full_name text not null default '',
  profile_photo_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.users
  add column if not exists auth_user_id uuid,
  add column if not exists email text,
  add column if not exists first_name text,
  add column if not exists last_name text,
  add column if not exists full_name text,
  add column if not exists profile_photo_url text,
  add column if not exists created_at timestamptz default now(),
  add column if not exists updated_at timestamptz default now();

create unique index if not exists users_auth_user_id_idx on public.users (auth_user_id);

-- Auto-create profile row on signup (optional but recommended)
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.users (auth_user_id, email, first_name, last_name, full_name)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data->>'first_name', ''),
    coalesce(new.raw_user_meta_data->>'last_name', ''),
    coalesce(new.raw_user_meta_data->>'full_name', '')
  )
  on conflict (auth_user_id) do update set
    email = excluded.email,
    updated_at = now();
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- Notifications (app queries recipient_user_id, read, created_at)
-- ---------------------------------------------------------------------------
create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  recipient_user_id uuid not null references auth.users (id) on delete cascade,
  title text not null default '',
  body text not null default '',
  read boolean not null default false,
  created_at timestamptz not null default now()
);

create index if not exists notifications_recipient_created_idx
  on public.notifications (recipient_user_id, created_at desc);

-- ---------------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------------
alter table public.users enable row level security;
alter table public.notifications enable row level security;

drop policy if exists users_select_own on public.users;
create policy users_select_own on public.users
  for select to authenticated
  using (auth.uid() = auth_user_id);

drop policy if exists users_insert_own on public.users;
create policy users_insert_own on public.users
  for insert to authenticated
  with check (auth.uid() = auth_user_id);

drop policy if exists users_update_own on public.users;
create policy users_update_own on public.users
  for update to authenticated
  using (auth.uid() = auth_user_id)
  with check (auth.uid() = auth_user_id);

drop policy if exists notifications_select_own on public.notifications;
create policy notifications_select_own on public.notifications
  for select to authenticated
  using (auth.uid() = recipient_user_id);

drop policy if exists notifications_update_own on public.notifications;
create policy notifications_update_own on public.notifications
  for update to authenticated
  using (auth.uid() = recipient_user_id)
  with check (auth.uid() = recipient_user_id);

-- Storage bucket for avatars (app: profile-pictures)
insert into storage.buckets (id, name, public)
values ('profile-pictures', 'profile-pictures', true)
on conflict (id) do nothing;
