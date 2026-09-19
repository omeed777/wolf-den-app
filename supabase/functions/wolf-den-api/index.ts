import { createClient } from "npm:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "GET,POST,DELETE,OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};
const url = Deno.env.get("SUPABASE_URL")!;
const publicKey = JSON.parse(Deno.env.get("SUPABASE_PUBLISHABLE_KEYS")!).default;
const secretKey = JSON.parse(Deno.env.get("SUPABASE_SECRET_KEYS")!).default;
const authClient = createClient(url, publicKey, { auth: { persistSession: false, autoRefreshToken: false } });
const admin = createClient(url, secretKey, { auth: { persistSession: false, autoRefreshToken: false } });

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: cors });

function normalizePhone(phone: string) {
  const digits = phone.replace(/\D/g, "");
  if (digits.startsWith("09")) return "+98" + digits.slice(1);
  if (digits.startsWith("98")) return "+" + digits;
  return digits.startsWith("+") ? digits : "+" + digits;
}

async function requireUser(req: Request) {
  const h = req.headers.get("Authorization") ?? "";
  const token = h.startsWith("Bearer ") ? h.slice(7) : "";
  if (!token) throw new Error("UNAUTHORIZED");
  const { data, error } = await authClient.auth.getUser(token);
  if (error || !data.user) throw new Error("UNAUTHORIZED");
  return data.user;
}

function fail(e: unknown) {
  const m = e instanceof Error ? e.message : "SERVER_ERROR";
  const status: Record<string, number> = {
    UNAUTHORIZED: 401, CLASS_NOT_FOUND: 404, SUBSCRIPTION_INACTIVE: 403,
    NO_SESSIONS: 409, CLASS_FULL: 409, ALREADY_BOOKED: 409, BOOKING_NOT_FOUND: 404,
  };
  return json({ message: m }, status[m] ?? 500);
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  const path = new URL(req.url).pathname.replace(/.*\/wolf-den-api/, "") || "/";
  try {
    if (req.method === "POST" && path === "/auth/request-otp") {
      const { phone } = await req.json();
      const { error } = await authClient.auth.signInWithOtp({ phone: normalizePhone(phone) });
      if (error) throw error;
      return json({ success: true });
    }

    if (req.method === "POST" && path === "/auth/verify-otp") {
      const { phone, code } = await req.json();
      const { data, error } = await authClient.auth.verifyOtp({
        phone: normalizePhone(phone), token: code, type: "sms",
      });
      if (error || !data.session || !data.user) throw error ?? new Error("INVALID_OTP");
      const { data: member } = await admin.from("members").select("name").eq("id", data.user.id).maybeSingle();
      return json({
        accessToken: data.session.access_token,
        memberId: data.user.id,
        name: member?.name ?? "",
      });
    }

    const user = await requireUser(req);

    if (req.method === "GET" && path === "/member") {
      const { data: member, error } = await admin.from("members")
        .select("id,name,phone").eq("id", user.id).single();
      if (error) throw error;
      const { data: subscription } = await admin.from("subscriptions")
        .select("plan,total_sessions,remaining_sessions,status,expires_at")
        .eq("member_id", user.id).order("created_at", { ascending: false }).limit(1).maybeSingle();
      return json({
        id: member.id, name: member.name, phone: member.phone,
        plan: subscription?.plan ?? "بدون اشتراک",
        totalSessions: subscription?.total_sessions ?? 0,
        remainingSessions: subscription?.remaining_sessions ?? 0,
        subscriptionStatus: subscription?.status ?? "expired",
        expiresAt: subscription?.expires_at ?? "",
      });
    }

    if (req.method === "GET" && path === "/classes") {
      const { data: classes, error } = await admin.from("classes")
        .select("id,public_id,title,class_date,start_time,capacity,coach_id")
        .eq("active", true).order("class_date").order("start_time");
      if (error) throw error;
      const ids = [...new Set((classes ?? []).map(c => c.coach_id).filter(Boolean))];
      const { data: coaches } = ids.length
        ? await admin.from("coaches").select("id,name").in("id", ids)
        : { data: [] };
      const coachMap = new Map((coaches ?? []).map(c => [c.id, c.name]));
      const classIds = (classes ?? []).map(c => c.id);
      const { data: bookings } = classIds.length
        ? await admin.from("bookings").select("class_id,status").in("class_id", classIds)
        : { data: [] };
      return json({ classes: (classes ?? []).map(c => ({
        id: c.public_id, title: c.title, day: c.class_date,
        time: String(c.start_time).slice(0, 5), capacity: c.capacity,
        booked: (bookings ?? []).filter(b => b.class_id === c.id && b.status === "booked").length,
        coach: coachMap.get(c.coach_id) ?? "Wolf Den",
      }))});
    }

    if (req.method === "GET" && path === "/bookings") {
      const { data, error } = await admin.from("bookings")
        .select("id,status,created_at,classes!inner(public_id)")
        .eq("member_id", user.id).eq("status", "booked").order("created_at", { ascending: false });
      if (error) throw error;
      return json({ bookings: (data ?? []).map((b: any) => ({
        id: b.id, classId: b.classes.public_id, status: b.status, createdAt: b.created_at,
      }))});
    }

    if (req.method === "POST" && path === "/bookings") {
      const { classId } = await req.json();
      const { data, error } = await admin.rpc("book_class", {
        p_member_id: user.id, p_public_id: Number(classId),
      });
      if (error) throw new Error(error.message);
      return json({ id: data.id, classId: Number(classId), status: data.status, createdAt: data.created_at });
    }

    const match = path.match(/^\/bookings\/(\d+)$/);
    if (req.method === "DELETE" && match) {
      const classId = Number(match[1]);
      const { data, error } = await admin.rpc("cancel_class", {
        p_member_id: user.id, p_public_id: classId,
      });
      if (error) throw new Error(error.message);
      return json({ success: true, id: data.id, classId, status: data.status, createdAt: data.created_at });
    }

    return json({ message: "NOT_FOUND" }, 404);
  } catch (e) {
    return fail(e);
  }
});
