-- Apply only in the dedicated NotificationVault Supabase project.
create table if not exists public.notifications (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    device_id text not null,
    package_name text not null,
    app_name text not null,
    title text not null default '',
    body text not null default '',
    posted_at timestamptz not null,
    created_at timestamptz not null default now()
);

alter table public.notifications enable row level security;

create policy "Users can read their own notifications"
on public.notifications for select to authenticated
using ((select auth.uid()) = user_id);

create policy "Users can insert their own notifications"
on public.notifications for insert to authenticated
with check ((select auth.uid()) = user_id);

create policy "Users can update their own notifications"
on public.notifications for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy "Users can delete their own notifications"
on public.notifications for delete to authenticated
using ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.notifications to authenticated;

create index if not exists notifications_user_posted_at_idx
on public.notifications (user_id, posted_at desc);
