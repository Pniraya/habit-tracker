// Verifies the DAO layer's SQL against a real SQLite engine.
//
// Gradle test workers are blocked in this sandbox, so Room cannot be launched. This harness runs
// the exact SQL the DAOs declare (parsed from the Kotlin sources) against the schema Room exported
// plus the triggers from DatabaseConstraints. It proves statement semantics and constraints.
// It does NOT prove Room's cursor mapping, converter round-trips or Flow invalidation - those need
// a device or Robolectric.
import { DatabaseSync } from "node:sqlite";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const schemaDir = join(root, "app", "schemas", "com.habittracker.data.local.HabitDatabase");
const schemaPath = join(schemaDir, "1.json");
const constraintsPath = join(root, "app", "src", "main", "java", "com", "habittracker", "data", "local", "DatabaseConstraints.kt");
const daoDir = join(root, "app", "src", "main", "java", "com", "habittracker", "data", "local", "dao");

const schema = JSON.parse(readFileSync(schemaPath, "utf8"));
const db = new DatabaseSync(":memory:");
db.exec("PRAGMA foreign_keys = ON");

for (const entity of schema.database.entities) {
  db.exec(entity.createSql.replace(/\$\{TABLE_NAME\}/g, entity.tableName));
  for (const index of entity.indices) {
    db.exec(index.createSql.replace(/\$\{TABLE_NAME\}/g, entity.tableName));
  }
}
for (const query of schema.database.setupQueries) db.exec(query);

const kotlin = readFileSync(constraintsPath, "utf8");
const triggerBlocks = Array.from(kotlin.matchAll(/"""([\s\S]*?)"""/g)).map((m) => m[1]);
let triggerCount = 0;
for (const block of triggerBlocks) {
  db.exec(block.split("\n").map((line) => line.replace(/^\s{8}/, "")).join("\n").trim());
  triggerCount += 1;
}

/** Extracts the SQL of every @Query in a DAO, keyed by the function name that follows it. */
function loadQueries(fileName) {
  const source = readFileSync(join(daoDir, fileName), "utf8");
  const queries = {};
  let index = source.indexOf("@Query(");
  while (index !== -1) {
    let cursor = index + "@Query(".length;
    let depth = 1;
    let inString = false;
    const parts = [];
    let current = "";
    while (cursor < source.length && depth > 0) {
      const ch = source[cursor];
      if (inString) {
        if (ch === "\\") {
          current += source[cursor + 1];
          cursor += 2;
          continue;
        }
        if (ch === '"') {
          parts.push(current);
          current = "";
          inString = false;
        } else {
          current += ch;
        }
      } else if (ch === '"') {
        inString = true;
      } else if (ch === "(") {
        depth += 1;
      } else if (ch === ")") {
        depth -= 1;
      }
      cursor += 1;
    }
    const rest = source.slice(cursor);
    const nameMatch = rest.match(/fun\s+(\w+)\s*\(/);
    if (nameMatch) queries[nameMatch[1]] = parts.join("");
    index = source.indexOf("@Query(", cursor);
  }
  return queries;
}

const habitQ = loadQueries("HabitDao.kt");
const logQ = loadQueries("HabitLogDao.kt");
const scheduleQ = loadQueries("HabitScheduleDao.kt");
const reminderQ = loadQueries("HabitReminderDao.kt");

let checks = 0;
let failed = 0;
function ok(label, fn) {
  checks += 1;
  try {
    const value = fn();
    console.log("PASS  " + label);
    return value;
  } catch (error) {
    failed += 1;
    console.log("FAIL  " + label + " -> " + error.message);
  }
  return undefined;
}
function equal(label, actual, expected) {
  ok(label + " (expected " + JSON.stringify(expected) + ")", () => {
    const a = JSON.stringify(actual);
    const e = JSON.stringify(expected);
    if (a !== e) throw new Error("got " + a);
  });
}

/** Binds Room-style named parameters (:name) to positional ones. */
function bind(sql, params) {
  if (typeof sql !== "string" || sql.length === 0) {
    throw new Error("query is missing: the DAO function name was not found in the Kotlin source");
  }
  const names = Array.from(sql.matchAll(/:(\w+)/g)).map((m) => m[1]);
  const unbound = names.filter((name) => !(name in params));
  if (unbound.length > 0) throw new Error("unbound parameters: " + unbound.join(", "));
  // Room expands "IN (:ids)" into one placeholder per element; mirror that here so the batch
  // queries are exercised with the same shape Room generates.
  const values = [];
  let positional = "";
  let cursor = 0;
  const parts = Array.from(sql.matchAll(/:(\w+)/g));
  for (const match of parts) {
    const name = match[1];
    const value = params[name];
    positional += sql.slice(cursor, match.index);
    if (Array.isArray(value)) {
      if (value.length === 0) throw new Error("empty list bound to :" + name + " would not match Room");
      positional += value.map(() => "?").join(", ");
      values.push(...value);
    } else {
      positional += "?";
      values.push(value);
    }
    cursor = match.index + match[0].length;
  }
  positional += sql.slice(cursor);
  return { positional, values };
}
function query(sql, params) {
  const bound = bind(sql, params);
  return db.prepare(bound.positional).all(...bound.values);
}

const insertHabit = db.prepare(
  "INSERT INTO habits (name, icon, color, tracking_type, target_value, unit, created_at, updated_at, created_on) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
);
const insertSchedule = db.prepare(
  "INSERT INTO habit_schedules (habit_id, frequency_type, weekday_mask, effective_from, effective_until, created_at) " +
    "VALUES (?, ?, ?, ?, ?, ?)",
);
const insertLog = db.prepare(
  "INSERT INTO habit_logs (habit_id, log_date, actual_value, target_snapshot, status, completed_at, duration_seconds, created_at, updated_at) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
);
const insertReminder = db.prepare(
  "INSERT INTO habit_reminders (habit_id, reminder_time, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
);

ok("every expected DAO query was parsed out of the Kotlin sources", () => {
  const expected = {
    HabitDao: [habitQ, ["findById", "observeActive", "observeArchived", "findAll", "archive", "restore", "updateFields", "deleteById", "deleteAll"]],
    HabitLogDao: [logQ, ["findByDate", "findRange", "observeRange", "findForDate", "findRangeForHabits", "findAllForBackup", "deleteAll"]],
    HabitReminderDao: [reminderQ, ["findForHabit", "findActiveForScheduling", "setEnabled", "findAllForBackup", "deleteAll"]],
    HabitScheduleDao: [
      scheduleQ,
      [
        "findVersionsOn",
        "findAll",
        "findOpenVersion",
        "closeVersion",
        "findAllForHabits",
        "findVersionsOnForHabits",
        "deleteAll",
      ],
    ],
  };
  const problems = [];
  const known = new Set();
  for (const [name, entry] of Object.entries(expected)) {
    const [queries, functions] = entry;
    for (const fn of functions) {
      known.add(name + "." + fn);
      if (!queries[fn]) problems.push("not parsed: " + name + "." + fn);
    }
    for (const fn of Object.keys(queries)) {
      if (!known.has(name + "." + fn)) problems.push("unexpected @Query not covered by a scenario: " + name + "." + fn);
    }
  }
  if (problems.length > 0) throw new Error(problems.join("; "));
});

// --- fixture: one active habit, one archived, plus a closed schedule version ---
const readId = insertHabit.run("Read", "book", "#5B8C6A", "BOOLEAN", 1, null, 10, 10, "2026-01-01").lastInsertRowid;
const waterId = insertHabit.run("Water", "drop", "#8FAFC4", "NUMERIC", 2000, "ml", 20, 20, "2026-01-02").lastInsertRowid;
insertSchedule.run(readId, "CUSTOM", 1, "2026-01-05", "2026-01-31", 10);
// The open version is the one closeVersion targets; keep its id instead of assuming fixture order.
const openVersionId = insertSchedule.run(readId, "DAILY", 127, "2026-02-01", null, 11).lastInsertRowid;
// 08:00:00, not 08:00: Converters writes HH:mm:ss, and the fixture must use what the app stores.
insertReminder.run(readId, "08:00:00", 1, 10, 10);
insertReminder.run(readId, "06:30:00", 1, 10, 10);
// A second habit with its own open version, so the batch queries are proved to span more than one id.
insertSchedule.run(waterId, "DAILY", 127, "2026-01-02", null, 20);

try {
// --- HabitDao ---
equal("HabitDao.observeActive hides archived rows", query(habitQ.observeActive, {}).length, 2);
equal("HabitDao.archive reports one changed row", queryOne(habitQ.archive, { id: waterId, archivedAt: 30, archivedOn: "2026-01-10", updatedAt: 30 }), 1);
equal("HabitDao.archive is a compare-and-set (second call changes nothing)", queryOne(habitQ.archive, { id: waterId, archivedAt: 31, archivedOn: "2026-01-11", updatedAt: 31 }), 0);
equal("HabitDao.observeActive after archive", query(habitQ.observeActive, {}).length, 1);
// TASK-028: the profile page lists exactly the archived rows, newest archive first.
equal("HabitDao.observeArchived lists only the archived habit", query(habitQ.observeArchived, {}).map((h) => h.id), [waterId]);

equal("HabitDao.findById returns the archived habit", query(habitQ.findById, { id: waterId })[0].archived_on, "2026-01-10");
// Restore last: it un-archives the row, so every assertion above needs it to still be archived.
equal("HabitDao.restore reports one changed row", queryOne(habitQ.restore, { id: waterId, updatedAt: 40 }), 1);
equal("HabitDao.observeArchived is empty after restore", query(habitQ.observeArchived, {}).length, 0);
equal("HabitDao.observeActive sees the restored habit again", query(habitQ.observeActive, {}).length, 2);

function queryOne(sql, params) {
  const bound = bind(sql, params);
  return db.prepare(bound.positional).run(...bound.values).changes;
}

// --- HabitLogDao ---
equal("HabitLogDao.findByDate is empty before the first write", query(logQ.findByDate, { habitId: readId, date: "2026-01-05" }).length, 0);
insertLog.run(readId, "2026-01-05", 1, 1, "COMPLETED", 100, null, 100, 100);
insertLog.run(readId, "2026-01-06", 1, 1, "COMPLETED", 110, null, 110, 110);
insertLog.run(readId, "2026-01-20", 0, 1, "IN_PROGRESS", null, null, 120, 120);
// TASK-030: the backup reads whole tables. Ordering matters so two exports of the same data match.
equal(
  "HabitLogDao.findAllForBackup returns every log ordered by habit then date",
  query(logQ.findAllForBackup, {}).map((r) => r.log_date),
  ["2026-01-05", "2026-01-06", "2026-01-20"],
);
equal(
  "HabitReminderDao.findAllForBackup returns every reminder ordered by habit then time",
  query(reminderQ.findAllForBackup, {}).map((r) => r.reminder_time),
  ["06:30:00", "08:00:00"],
);
equal("HabitLogDao.findByDate returns the single row", query(logQ.findByDate, { habitId: readId, date: "2026-01-05" })[0].status, "COMPLETED");
equal(
  "HabitLogDao.findRange includes both boundaries and sorts ascending",
  query(logQ.findRange, { habitId: readId, from: "2026-01-05", to: "2026-01-06" }).map((r) => r.log_date),
  ["2026-01-05", "2026-01-06"],
);
equal(
  "HabitLogDao.findRange excludes rows outside the window",
  query(logQ.findRange, { habitId: readId, from: "2026-01-06", to: "2026-01-19" }).map((r) => r.log_date),
  ["2026-01-06"],
);
equal(
  "HabitLogDao.observeRange spans habits and keeps ascending order",
  query(logQ.observeRange, { from: "2026-01-05", to: "2026-01-06" }).map((r) => r.log_date),
  ["2026-01-05", "2026-01-06"],
);
ok("a duplicate log for the same day is rejected by the unique index", () => {
  try {
    insertLog.run(readId, "2026-01-05", 1, 1, "COMPLETED", 100, null, 130, 130);
  } catch (error) {
    if (!/UNIQUE/i.test(String(error.message))) throw error;
    return;
  }
  throw new Error("duplicate log was accepted");
});
ok("the DAO's upsert shape updates instead of duplicating", () => {
  db.exec(
    "INSERT INTO habit_logs (habit_id, log_date, actual_value, target_snapshot, status, completed_at, duration_seconds, created_at, updated_at) " +
      "VALUES (" + readId + ", '2026-01-05', 1, 1, 'COMPLETED', 140, NULL, 140, 140) " +
      "ON CONFLICT(habit_id, log_date) DO UPDATE SET actual_value = excluded.actual_value, updated_at = excluded.updated_at",
  );
  const rows = query(logQ.findByDate, { habitId: readId, date: "2026-01-05" });
  if (rows.length !== 1) throw new Error("expected 1 row, got " + rows.length);
  if (rows[0].updated_at !== 140) throw new Error("row was not updated");
});

// --- HabitScheduleDao ---
equal("HabitScheduleDao.findVersionsOn inside the closed version", query(scheduleQ.findVersionsOn, { habitId: readId, date: "2026-01-31" }).length, 1);
equal("HabitScheduleDao.findVersionsOn on the first day of the open version", query(scheduleQ.findVersionsOn, { habitId: readId, date: "2026-02-01" }).length, 1);
equal("HabitScheduleDao.findVersionsOn before any version", query(scheduleQ.findVersionsOn, { habitId: readId, date: "2026-01-04" }).length, 0);
equal("HabitScheduleDao.findVersionsOn after the closed range has exactly one row", query(scheduleQ.findVersionsOn, { habitId: readId, date: "2026-06-01" }).length, 1);
equal("HabitScheduleDao.closeVersion reports one changed row", queryOne(scheduleQ.closeVersion, { id: openVersionId, until: "2026-11-04" }), 1);
equal("HabitScheduleDao.closeVersion is a compare-and-set (second call changes nothing)", queryOne(scheduleQ.closeVersion, { id: openVersionId, until: "2026-11-05" }), 0);
equal("HabitScheduleDao.findOpenVersion has no open version left", query(scheduleQ.findOpenVersion, { habitId: readId }).length, 0);

// --- batch forms (TASK-022): one query for the whole Today list ---
equal(
  "HabitScheduleDao.findAllForHabits returns every version of every id",
  query(scheduleQ.findAllForHabits, { habitIds: [readId, waterId] }).length,
  3,
);
equal(
  "HabitScheduleDao.findAllForHabits respects the id list",
  query(scheduleQ.findAllForHabits, { habitIds: [waterId] }).map((r) => r.habit_id),
  [waterId],
);
equal(
  "HabitScheduleDao.findVersionsOnForHabits answers for each id on a date",
  query(scheduleQ.findVersionsOnForHabits, { habitIds: [readId, waterId], date: "2026-02-01" }).length,
  2,
);
equal(
  "HabitScheduleDao.findVersionsOnForHabits skips habits with no version yet",
  query(scheduleQ.findVersionsOnForHabits, { habitIds: [readId, waterId], date: "2026-01-01" }).length,
  0,
);
insertLog.run(waterId, "2026-01-05", 500, 2000, "IN_PROGRESS", null, null, 150, 150);

// TASK-029: editing a habit must not rewrite history. The log above stores target_snapshot = 2000;
// after the habit target changes to 1500 the snapshot has to stay 2000, or past completion rates
// would silently change.
equal("HabitDao.updateFields reports one changed row", queryOne(habitQ.updateFields, { id: waterId, name: "Water (big)", targetValue: 1500, unit: "ml", updatedAt: 160 }), 1);
equal("HabitDao.updateFields changes the habit target", query(habitQ.findById, { id: waterId })[0].target_value, 1500);
equal("HabitDao.updateFields changes the habit name", query(habitQ.findById, { id: waterId })[0].name, "Water (big)");
equal("HabitDao.updateFields leaves the log snapshot alone", db.prepare("SELECT target_snapshot FROM habit_logs WHERE habit_id = ? AND log_date = ?").get(waterId, "2026-01-05").target_snapshot, 2000);
equal("HabitDao.updateFields refuses an archived habit", (queryOne(habitQ.archive, { id: waterId, archivedAt: 170, archivedOn: "2026-01-31", updatedAt: 170 }) === 1) && queryOne(habitQ.updateFields, { id: waterId, name: "x", targetValue: 1, unit: null, updatedAt: 180 }) === 0, true);
equal("HabitDao.updateFields is a no-op for an unknown id", queryOne(habitQ.updateFields, { id: 999999, name: "x", targetValue: 1, unit: null, updatedAt: 190 }), 0);
equal(
  "HabitLogDao.findForDate spans habits and keeps only that date",
  query(logQ.findForDate, { habitIds: [readId, waterId], date: "2026-01-05" }).length,
  2,
);
equal(
  "HabitLogDao.findRangeForHabits keeps both boundaries and filters by id",
  query(logQ.findRangeForHabits, { habitIds: [readId, waterId], from: "2026-01-05", to: "2026-01-06" })
    .map((r) => r.habit_id + "@" + r.log_date)
    .sort(),
  [readId + "@2026-01-05", readId + "@2026-01-06", waterId + "@2026-01-05"].sort(),
);

// --- cascade (TASK-034: the delete action runs this exact DAO statement) ---
ok("HabitDao.deleteById removes the habit and cascades to schedules, logs and reminders", () => {
  const deleted = queryOne(habitQ.deleteById, { id: readId });
  if (deleted !== 1) throw new Error("expected 1 deleted row, got " + deleted);
  const counts = ["habit_schedules", "habit_logs", "habit_reminders"].map(
    (table) => db.prepare("SELECT COUNT(*) AS c FROM " + table + " WHERE habit_id = ?").get(readId).c,
  );
  if (counts.some((count) => count !== 0)) throw new Error("orphan rows remain: " + JSON.stringify(counts));
});
equal("HabitDao.deleteById is a no-op for an unknown id", queryOne(habitQ.deleteById, { id: 999999 }), 0);

// --- restore path (TASK-030b, ADR-019): the four deleteAll statements must empty their tables ---
insertReminder.run(waterId, "21:00:00", 1, 10, 10);
equal("HabitReminderDao.deleteAll empties the reminder table", queryOne(reminderQ.deleteAll, {}), 1);
equal("HabitLogDao.deleteAll empties the log table", queryOne(logQ.deleteAll, {}), 1);
equal("HabitScheduleDao.deleteAll empties the schedule table", queryOne(scheduleQ.deleteAll, {}), 1);
equal("HabitDao.deleteAll empties the habit table", queryOne(habitQ.deleteAll, {}), 1);
equal("every table is empty after the restore wipes", ["habits", "habit_schedules", "habit_logs", "habit_reminders"].map((table) => db.prepare("SELECT COUNT(*) AS c FROM " + table).get().c), [0, 0, 0, 0]);

} catch (error) {
  // A parse or bind failure must still produce a summary instead of a bare stack trace.
  failed += 1;
  console.log("FAIL  uncaught: " + error.message);
}

// Deleting a scenario while keeping its registration would otherwise shrink coverage silently.
// Bump this number in the same commit that adds a scenario.
const MIN_CHECKS = 45;
if (checks < MIN_CHECKS) {
  failed += 1;
  console.log("FAIL  coverage shrank: " + checks + " checks executed, at least " + MIN_CHECKS + " expected");
}

console.log("");
console.log("triggers installed: " + triggerCount);
console.log("checks: " + checks + ", failed: " + failed);
if (failed > 0) {
  console.log("RESULT: DAO SQL FAILED");
  process.exit(1);
}
console.log("RESULT: DAO SQL PASS");
