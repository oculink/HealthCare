#!/usr/bin/env python3
"""Seed CareApp demo accounts with realistic history for screenshots / ads.

Creates (or reuses) username accounts in Firebase Auth for the project in
app/google-services.json, then writes the matching `users/{uid}` profile,
`users/{uid}/readings` history (past weeks), medications with an adherence log,
and appointments - through the same paths and rules the app itself writes to.

    python3 seed_demo_accounts.py              # all personas
    python3 seed_demo_accounts.py hassan        # just one
    python3 seed_demo_accounts.py --reset       # wipe readings/meds/appts first

Nothing is written to `stats/vitals` (the community aggregate), so the demo data
never skews the "you vs everyone" numbers.

Sign in in the app with the username and PASSWORD below.
"""

import json
import random
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

PROJECT = "finalyearproject-f1e53"
API_KEY = "AIzaSyCUbYSPfpawq4SfNLWkqKx_r7PX7CkumoA"
PASSWORD = "CareDemo2026"
DOMAIN = "@careapp.invalid"
DAYS = 42                                    # six weeks of readings
TZ = timezone(timedelta(hours=8))            # Malaysia

AUTH = "https://identitytoolkit.googleapis.com/v1/accounts:"
FS = f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents"

PERSONAS = [
    {
        "username": "aisyah",
        "profile": {
            "name": "Aisyah Rahman",
            "birthDate": "1992-03-14",
            "sex": "Female",
            "heightCm": "158",
            "weightKg": "62",
            "bloodType": "O+",
            "allergies": ["Penicillin"],
            "conditions": ["Hypertension"],
            "emergencyContacts": [
                {"name": "Farid Rahman", "relation": "Husband", "phone": "012-3456789"}
            ],
        },
        "stepGoal": 8000,
        "steps": 7420,
        "sleep": {"hours": 7.2, "quality": "Good"},
        "times": ["07:40", "12:30", "18:10", "21:30"],
        "vitals": {"hr": (70, 78), "sys": (116, 126), "dia": (74, 82),
                   "sugar": (90, 104), "temp": (36.4, 36.7), "ox": (97, 99)},
        "trend": {"sys": -6, "sugar": -4},
        "warn_days": {9: {"sys": 139, "dia": 88}, 26: {"hr": 96}},
        "meds": [
            {"name": "Amlodipine", "strength": "5 mg", "times": ["08:00"],
             "desc": "Blood pressure · one tablet with breakfast", "missed": []},
        ],
        "appointments": [
            {"doctor": "Dr. Lim Wei Shan", "specialty": "Family Medicine",
             "clinic": "Klinik Kesihatan Wangsa Maju", "address": "Jalan Wangsa Delima, KL",
             "days": 5, "time": "10:30", "notes": "Monthly blood pressure review",
             "status": "upcoming"},
            {"doctor": "Dr. Lim Wei Shan", "specialty": "Family Medicine",
             "clinic": "Klinik Kesihatan Wangsa Maju", "address": "Jalan Wangsa Delima, KL",
             "days": -26, "time": "10:30", "notes": "Reviewed readings, continue current dose",
             "status": "completed"},
        ],
    },
    {
        "username": "hassan",
        "profile": {
            "name": "Hassan bin Ismail",
            "birthDate": "1963-11-02",
            "sex": "Male",
            "heightCm": "170",
            "weightKg": "78",
            "bloodType": "A+",
            "allergies": ["Aspirin"],
            "conditions": ["Type 2 diabetes", "Hypertension"],
            "emergencyContacts": [
                {"name": "Nurul Aina", "relation": "Daughter", "phone": "013-8899001"}
            ],
        },
        "stepGoal": 6000,
        "steps": 4180,
        "sleep": {"hours": 6.1, "quality": "Fair"},
        "times": ["07:20", "12:10", "18:40", "21:50"],
        "vitals": {"hr": (74, 86), "sys": (130, 142), "dia": (80, 88),
                   "sugar": (128, 168), "temp": (36.5, 36.9), "ox": (95, 98)},
        "trend": {"sugar": -18, "sys": -4},
        "warn_days": {6: {"sugar": 214, "sys": 151, "dia": 93},
                      17: {"sugar": 196},
                      31: {"sys": 158, "dia": 96}},
        "meds": [
            {"name": "Metformin", "strength": "500 mg", "times": ["08:00", "20:00"],
             "desc": "For blood sugar · take with food", "missed": [3, 11, 19]},
            {"name": "Amlodipine", "strength": "5 mg", "times": ["08:00"],
             "desc": "Blood pressure · one tablet with breakfast", "missed": [7]},
            {"name": "Atorvastatin", "strength": "20 mg", "times": ["21:00"],
             "desc": "Cholesterol · one tablet at night", "missed": []},
        ],
        "appointments": [
            {"doctor": "Dr. Tan Chee Keong", "specialty": "Endocrinology",
             "clinic": "Hospital Kuala Lumpur", "address": "Jalan Pahang, 50586 KL",
             "days": 3, "time": "09:00", "notes": "HbA1c result + medication review",
             "status": "upcoming"},
            {"doctor": "Dr. Tan Chee Keong", "specialty": "Endocrinology",
             "clinic": "Hospital Kuala Lumpur", "address": "Jalan Pahang, 50586 KL",
             "days": 10, "time": "14:15", "notes": "Diabetes education session",
             "status": "upcoming"},
        ],
    },
    {
        "username": "meiling",
        "profile": {
            "name": "Mei Ling Tan",
            "birthDate": "1997-07-21",
            "sex": "Female",
            "heightCm": "165",
            "weightKg": "55",
            "bloodType": "B+",
            "allergies": [],
            "conditions": [],
            "emergencyContacts": [
                {"name": "Tan Wei Ming", "relation": "Brother", "phone": "016-7722110"}
            ],
        },
        "stepGoal": 10000,
        "steps": 11240,
        "sleep": {"hours": 7.6, "quality": "Good"},
        "times": ["08:00", "20:30"],
        "vitals": {"hr": (58, 68), "sys": (106, 116), "dia": (66, 74),
                   "sugar": (82, 94), "temp": (36.4, 36.7), "ox": (98, 100)},
        "trend": {"hr": -2},
        "warn_days": {},
        "meds": [
            {"name": "Vitamin D3", "strength": "1000 IU", "times": ["09:00"],
             "desc": "Daily supplement", "missed": []},
        ],
        "appointments": [],
    },
]


# ---------------------------------------------------------------- REST helpers

def _call(url, payload=None, method="POST", token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(
        url, data=json.dumps(payload).encode() if payload is not None else None,
        headers=headers, method=method,
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            body = r.read()
            return json.loads(body) if body else {}
    except urllib.error.HTTPError as e:
        raise SystemExit(f"HTTP {e.code} on {url.split('?')[0]}\n{e.read().decode()}")


def sign_in(username, create=False):
    email = username + DOMAIN
    if create:
        try:
            return _call(f"{AUTH}signUp?key={API_KEY}",
                         {"email": email, "password": PASSWORD, "returnSecureToken": True})
        except SystemExit as e:
            if "EMAIL_EXISTS" not in str(e):
                raise
    return _call(f"{AUTH}signInWithPassword?key={API_KEY}",
                 {"email": email, "password": PASSWORD, "returnSecureToken": True})


def fs_patch(path, fields, token):
    return _call(f"{FS}/{path}", {"fields": fields}, method="PATCH", token=token)


def fs_add(collection, fields, token):
    return _call(f"{FS}/{collection}", {"fields": fields}, token=token)


def fs_delete_all(uid, collection, token):
    rows = _call(f"{FS}/users/{uid}:runQuery",
                 {"structuredQuery": {"from": [{"collectionId": collection}],
                                      "select": {"fields": [{"fieldPath": "__name__"}]}}},
                 token=token)
    n = 0
    for r in rows:
        name = r.get("document", {}).get("name")
        if name:
            _call(f"https://firestore.googleapis.com/v1/{name}", method="DELETE", token=token)
            n += 1
    return n


# ------------------------------------------------------------- value encoders

def s(v):       return {"stringValue": str(v)}
def d(v):       return {"doubleValue": float(v)}
def i(v):       return {"integerValue": str(int(v))}
def ts(sec):    return {"timestampValue": datetime.fromtimestamp(sec, timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")}
def arr_str(xs): return {"arrayValue": {"values": [s(x) for x in xs] if xs else []}}
def arr_of_maps(ds): return {"arrayValue": {"values": [map_(x) for x in ds]} if ds else []}
def map_(dd):   return {"mapValue": {"fields": dd}}


# ------------------------------------------------------------------- seeding

def day_stamps(p, rng):
    """(day_index, datetime) for every reading slot, skipping anything in the future."""
    now = datetime.now(TZ)
    out = []
    for day in range(DAYS):
        date = (now - timedelta(days=DAYS - 1 - day)).date()
        for hhmm in p["times"]:
            hh, mm = (int(x) for x in hhmm.split(":"))
            stamp = datetime.combine(date, datetime.min.time(), TZ).replace(hour=hh, minute=mm)
            stamp += timedelta(minutes=rng.randint(-9, 9))
            if stamp <= now - timedelta(minutes=20):
                out.append((day, stamp))
    # make sure today has at least one reading, so Home never shows yesterday's
    if not any(st.date() == now.date() for _, st in out):
        out.append((DAYS - 1, now - timedelta(minutes=45)))
    return out


def build_readings(p, rng):
    vit, trend = p["vitals"], p.get("trend", {})
    rows = []
    for day, stamp in day_stamps(p, rng):
        t = day / max(DAYS - 1, 1)
        lo_hi = lambda k: vit[k][0] + (vit[k][1] - vit[k][0]) * rng.random()
        hr = int(lo_hi("hr") + trend.get("hr", 0) * (t - 1))
        sys_ = int(lo_hi("sys") + trend.get("sys", 0) * (t - 1))
        dia = int(lo_hi("dia") + trend.get("dia", 0) * (t - 1))
        sugar = int(lo_hi("sugar") + trend.get("sugar", 0) * (t - 1))
        temp = round(lo_hi("temp"), 1)
        ox = int(lo_hi("ox"))

        over = p["warn_days"].get(day)
        if over and stamp.hour < 12:
            hr = over.get("hr", hr)
            sys_ = over.get("sys", sys_)
            dia = over.get("dia", dia)
            sugar = over.get("sugar", sugar)
            ox = over.get("ox", ox)

        epoch = int(stamp.timestamp())
        rows.append({
            "bloodPressure": s(f"{sys_}/{dia}"),
            "bloodSugar": s(str(sugar)),
            "heartRate": s(str(hr)),
            "temperature": s(f"{temp:.1f}"),
            "oxygen": s(str(ox)),
            "clientTime": i(epoch * 1000),
            "recordedAt": ts(epoch),
        })
    return rows


def build_meds(p, rng, now):
    """One doc per medication, with a 'taken'/'missed' log for the last 14 days."""
    rows = []
    for n, med in enumerate(p["meds"]):
        med_id = 1000 + n
        schedule = {str(day): med["times"] for day in range(7)}
        log = {}
        for back in range(14):
            date = (now - timedelta(days=back)).date()
            for slot_n, hhmm in enumerate(med["times"]):
                hh, mm = (int(x) for x in hhmm.split(":"))
                when = datetime.combine(date, datetime.min.time(), TZ).replace(hour=hh, minute=mm)
                if when > now:            # never mark a dose that has not come round yet
                    continue
                # deterministic light history: mostly taken, the listed days missed
                if back in med["missed"]:
                    log[f"{date} {hhmm}"] = "missed"
                elif rng.random() > 0.06:
                    log[f"{date} {hhmm}"] = "taken"
        rows.append({
            "id": i(med_id),
            "name": s(med["name"]),
            "strength": s(med["strength"]),
            "route": s("ORAL"),
            "amount": i(1),
            "consumesPerDay": i(len(med["times"])),
            "schedule": map_({k: arr_str(v) for k, v in schedule.items()}),
            "scheduledDays": arr_str([0, 1, 2, 3, 4, 5, 6]),
            "log": map_({k: s(v) for k, v in log.items()}),
            "description": s(med["desc"]),
            "updatedAt": ts(int(now.timestamp())),
        })
    return rows


def build_appointments(p, now):
    rows = []
    for n, a in enumerate(p["appointments"]):
        hh, mm = (int(x) for x in a["time"].split(":"))
        start = (now + timedelta(days=a["days"])).replace(hour=hh, minute=mm, second=0, microsecond=0)
        appt_id = 2000 + n
        rows.append({
            "id": i(appt_id),
            "doctorName": s(a["doctor"]),
            "specialty": s(a["specialty"]),
            "clinicName": s(a["clinic"]),
            "address": s(a["address"]),
            "startMillis": i(int(start.timestamp() * 1000)),
            "notes": s(a["notes"]),
            "remindMinutesBefore": i(60),
            "status": s(a["status"]),
            "createdAt": i(int((now - timedelta(days=20)).timestamp() * 1000)),
        })
    return rows


def seed(p, reset):
    rng = random.Random(p["username"])
    auth = sign_in(p["username"], create=True)
    uid, token = auth["localId"], auth["idToken"]
    _call(f"{AUTH}update?key={API_KEY}", {"idToken": token, "displayName": p["profile"]["name"]})

    now = datetime.now(TZ)
    steps_day = now.strftime("%Y-%m-%d")
    sleep_start = int((now - timedelta(hours=p["sleep"]["hours"] + 1)).timestamp())
    sleep_end = int((now - timedelta(hours=1)).timestamp())

    doc = {
        "name": s(p["profile"]["name"]),
        "birthDate": s(p["profile"]["birthDate"]),
        "sex": s(p["profile"]["sex"]),
        "heightCm": s(p["profile"]["heightCm"]),
        "weightKg": s(p["profile"]["weightKg"]),
        "bloodType": s(p["profile"]["bloodType"]),
        "allergies": arr_str(p["profile"]["allergies"]),
        "conditions": arr_str(p["profile"]["conditions"]),
        "emergencyContacts": arr_of_maps([
            {"name": s(c["name"]), "relation": s(c["relation"]), "phone": s(c["phone"])}
            for c in p["profile"]["emergencyContacts"]
        ]),
        "role": s("patient"),
        "accountName": s(p["profile"]["name"]),
        "stepGoal": i(p["stepGoal"]),
        "stepsByDevice": map_({"demo": map_({"c": i(p["steps"]), "d": s(steps_day)})}),
        "sleepByDevice": map_({"demo": map_({
            "s": i(sleep_start), "e": i(sleep_end),
            "q": s(p["sleep"]["quality"]), "src": s("phone"), "awk": i(1),
        })}),
        "profileUpdatedAt": ts(int(now.timestamp())),
    }
    fs_patch(f"users/{uid}", doc, token)

    removed = 0
    if reset:
        for col in ("readings", "medications", "appointments"):
            removed += fs_delete_all(uid, col, token)

    readings = build_readings(p, rng)
    for r in readings:
        fs_add(f"users/{uid}/readings", r, token)

    meds = build_meds(p, rng, now)
    for m in meds:
        fs_patch(f"users/{uid}/medications/{m['id']['integerValue']}", m, token)

    appts = build_appointments(p, now)
    for a in appts:
        fs_patch(f"users/{uid}/appointments/{a['id']['integerValue']}", a, token)

    print(f"  {p['username']:8s} uid={uid}  {len(readings)} readings, "
          f"{len(meds)} meds, {len(appts)} appointments"
          + (f"  (removed {removed} old docs)" if reset else ""))
    return uid


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    reset = "--reset" in sys.argv[1:]
    print(f"Seeding {DAYS} days of history into {PROJECT} …")
    for p in PERSONAS:
        if args and p["username"] not in args:
            continue
        print(f"- {p['profile']['name']}")
        seed(p, reset)
    print(f"\nDone. Usernames: {', '.join(p['username'] for p in PERSONAS)}"
          f"\nPassword: {PASSWORD}")


if __name__ == "__main__":
    main()
