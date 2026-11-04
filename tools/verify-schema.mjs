// Verifies the exported Room schema (DATABASE.md 4) against a real SQLite engine.
// Gradle test workers cannot run in this sandbox, so constraint behaviour is proven here with
// node:sqlite using exactly the DDL Room exported plus the trigger DDL from DatabaseConstraints.
import { DatabaseSync } from "node:sqlite";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const schemaPath = join(root, "app", "schemas", "com.habittracker.data.local.HabitDatabase", "1.json");
const constraintsPath = join(root, "app", "src", "main", "java", "com", "habittracker", "data", "local", "DatabaseConstraints.kt");

const schema = JSON.parse(readFileSync(schemaPath, "utf8"));
const db = new DatabaseSync(":memory:");
db.exec("PRAGMA foreign_keys = ON");

for (const entity of schema.database.entities) {
  db.exec(entity.createSql.replace(/\$\{TABLE_NAME\}/g, entity.tableName));
  for (const index of entity.indices) {
    db.exec(index.createSql.replace(/\$\{TABLE_NAME\}/g, entity.tableName));
  }
}
for (const query of schema.database.setupQueries) {
  db.exec(query);
}

const kotlin = readFileSync(constraintsPath, "utf8");
const blocks = Array.from(kotlin.matchAll(/"""([\s\S]*?)"""/g)).map((m) => m[1]);
let triggerCount = 0;
for (const block of blocks) {
  const sql = block.split("\n").map((line) => line.replace(/^\s{8}/, "")).join("\n").trim();
  db.exec(sql);
  triggerCount += 1;
}

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
function rejects(label, fn) {
  checks += 1;
  try {
    fn();
    failed += 1;
    console.log("FAIL  " + label + " (statement was accepted)");
  } catch (error) {
    console.log("PASS  " + label + " -> " + String(error.message).slice(0, 70));
  }
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

// --- habits ---
const booleanHabitId = ok("habits: BOOLEAN habit accepted", () =>
  insertHabit.run("Read", "book", "#5B8C6A", "BOOLEAN", 1, null, 1, 1, "2026-11-05"),
);
const numericId = insertHabit.run("Water", "drop", "#8FAFC4", "NUMERIC", 2000, "ml", 1, 1, "2026-11-05").lastInsertRowid;
const otherId = insertHabit.run("Walk", "walk", "#F2A65A", "BOOLEAN", 1, null, 1, 1, "2026-11-05").lastInsertRowid;

rejects("habits: blank name rejected", () =>
  insertHabit.run("   ", "book", "#000000", "BOOLEAN", 1, null, 1, 1, "2026-11-05"),
);
rejects("habits: BOOLEAN with unit rejected", () =>
  insertHabit.run("Bad", "book", "#000000", "BOOLEAN", 1, "ml", 1, 1, "2026-11-05"),
);
rejects("habits: NUMERIC without unit rejected", () =>
  insertHabit.run("Bad", "book", "#000000", "NUMERIC", 5, "   ", 1, 1, "2026-11-05"),
);
rejects("habits: target_value 0 rejected", () =>
  insertHabit.run("Bad", "book", "#000000", "BOOLEAN", 0, null, 1, 1, "2026-11-05"),
);
rejects("habits: unknown tracking_type rejected", () =>
  insertHabit.run("Bad", "book", "#000000", "COUNTABLE", 1, null, 1, 1, "2026-11-05"),
);

// --- habit_schedules ---
ok("schedules: DAILY with mask 127 accepted", () =>
  insertSchedule.run(booleanHabitId.lastInsertRowid, "DAILY", 127, "2026-11-05", null, 1),
);
ok("schedules: CUSTOM with mask 21 accepted", () =>
  insertSchedule.run(numericId, "CUSTOM", 21, "2026-11-05", null, 1),
);
rejects("schedules: DAILY with mask 21 rejected", () =>
  insertSchedule.run(otherId, "DAILY", 21, "2026-11-05", null, 1),
);
rejects("schedules: WEEKDAYS with mask 127 rejected", () =>
  insertSchedule.run(otherId, "WEEKDAYS", 127, "2026-11-05", null, 1),
);
rejects("schedules: mask 200 rejected", () =>
  insertSchedule.run(otherId, "CUSTOM", 200, "2026-11-05", null, 1),
);
rejects("schedules: effective_until before effective_from rejected", () =>
  insertSchedule.run(otherId, "DAILY", 127, "2026-11-10", "2026-11-01", 1),
);
rejects("schedules: unknown frequency rejected", () =>
  insertSchedule.run(otherId, "MONTHLY", 127, "2026-11-05", null, 1),
);

// --- habit_logs ---
ok("logs: completed log accepted", () =>
  insertLog.run(booleanHabitId.lastInsertRowid, "2026-11-05", 1, 1, "COMPLETED", 1762300000000, null, 1, 1),
);
ok("logs: numeric partial log accepted", () =>
  insertLog.run(numericId, "2026-11-05", 800, 2000, "IN_PROGRESS", null, null, 1, 1),
);
rejects("logs: COMPLETED with actual below target rejected", () =>
  insertLog.run(numericId, "2026-11-06", 500, 2000, "COMPLETED", 1762300000000, null, 1, 1),
);
rejects("logs: IN_PROGRESS carrying completed_at rejected", () =>
  insertLog.run(numericId, "2026-11-07", 500, 2000, "IN_PROGRESS", 1762300000000, null, 1, 1),
);
rejects("logs: duplicate habit+date rejected", () =>
  insertLog.run(booleanHabitId.lastInsertRowid, "2026-11-05", 0, 1, "IN_PROGRESS", null, null, 1, 1),
);
rejects("logs: BOOLEAN habit with value 0.5 rejected", () =>
  insertLog.run(booleanHabitId.lastInsertRowid, "2026-11-08", 0.5, 1, "IN_PROGRESS", null, null, 1, 1),
);
rejects("logs: negative actual value rejected", () =>
  insertLog.run(numericId, "2026-11-09", -1, 2000, "IN_PROGRESS", null, null, 1, 1),
);
rejects("logs: direct UPDATE to COMPLETED below target rejected", () =>
  db.exec("UPDATE habit_logs SET status = 'COMPLETED' WHERE habit_id = " + numericId + " AND log_date = '2026-11-05'"),
);
rejects("logs: FK to missing habit rejected", () =>
  insertLog.run(9999, "2026-11-05", 1, 1, "COMPLETED", 1, null, 1, 1),
);

// --- habit_reminders ---
ok("reminders: first reminder accepted", () =>
  insertReminder.run(booleanHabitId.lastInsertRowid, "08:00:00", 1, 1, 1),
);
ok("reminders: same time on another habit accepted", () =>
  insertReminder.run(otherId, "08:00:00", 1, 1, 1),
);
rejects("reminders: duplicate habit+time rejected", () =>
  insertReminder.run(booleanHabitId.lastInsertRowid, "08:00:00", 1, 1, 1),
);
rejects("reminders: enabled=2 rejected", () =>
  insertReminder.run(booleanHabitId.lastInsertRowid, "09:00:00", 2, 1, 1),
);

// --- archive semantics (ADR-008) ---
ok("archive: keeps schedules, logs and reminders, leaves enabled untouched", () => {
  db.exec("UPDATE habits SET archived_on = '2026-11-05', archived_at = 1762300000000 WHERE id = " + booleanHabitId.lastInsertRowid);
  const children = db.prepare(
    "SELECT (SELECT COUNT(*) FROM habit_schedules WHERE habit_id = ?) AS schedules, " +
      "(SELECT COUNT(*) FROM habit_logs WHERE habit_id = ?) AS logs, " +
      "(SELECT COUNT(*) FROM habit_reminders WHERE habit_id = ? AND enabled = 1) AS reminders",
  ).get(booleanHabitId.lastInsertRowid, booleanHabitId.lastInsertRowid, booleanHabitId.lastInsertRowid);
  if (children.schedules !== 1 || children.logs !== 1 || children.reminders !== 1) {
    throw new Error("children lost: " + JSON.stringify(children));
  }
});

ok("cascade: deleting a habit removes its children", () => {
  db.exec("DELETE FROM habits WHERE id = " + booleanHabitId.lastInsertRowid);
  const left = db.prepare(
    "SELECT (SELECT COUNT(*) FROM habit_schedules WHERE habit_id = ?) + " +
      "(SELECT COUNT(*) FROM habit_logs WHERE habit_id = ?) + " +
      "(SELECT COUNT(*) FROM habit_reminders WHERE habit_id = ?) AS total",
  ).get(booleanHabitId.lastInsertRowid, booleanHabitId.lastInsertRowid, booleanHabitId.lastInsertRowid);
  if (left.total !== 0) { throw new Error("orphan rows: " + left.total); }
});

// --- exported schema parity with DATABASE.md 4 ---
const expectedColumns = {
  habits: [
    "id|INTEGER|true|undefined",
    "name|TEXT|true|undefined",
    "icon|TEXT|true|undefined",
    "color|TEXT|true|undefined",
    "tracking_type|TEXT|true|undefined",
    "target_value|REAL|true|1.0",
    "unit|TEXT|false|undefined",
    "created_at|INTEGER|true|undefined",
    "updated_at|INTEGER|true|undefined",
    "archived_at|INTEGER|false|undefined",
    "created_on|TEXT|true|undefined",
    "archived_on|TEXT|false|undefined",
  ],
  habit_schedules: [
    "id|INTEGER|true|undefined",
    "habit_id|INTEGER|true|undefined",
    "frequency_type|TEXT|true|undefined",
    "weekday_mask|INTEGER|true|undefined",
    "effective_from|TEXT|true|undefined",
    "effective_until|TEXT|false|undefined",
    "created_at|INTEGER|true|undefined",
  ],
  habit_logs: [
    "id|INTEGER|true|undefined",
    "habit_id|INTEGER|true|undefined",
    "log_date|TEXT|true|undefined",
    "actual_value|REAL|true|undefined",
    "target_snapshot|REAL|true|undefined",
    "status|TEXT|true|undefined",
    "completed_at|INTEGER|false|undefined",
    "duration_seconds|INTEGER|false|undefined",
    "created_at|INTEGER|true|undefined",
    "updated_at|INTEGER|true|undefined",
  ],
  habit_reminders: [
    "id|INTEGER|true|undefined",
    "habit_id|INTEGER|true|undefined",
    "reminder_time|TEXT|true|undefined",
    "enabled|INTEGER|true|1",
    "created_at|INTEGER|true|undefined",
    "updated_at|INTEGER|true|undefined",
  ],
};

const expectedIndices = {
  habits: [],
  habit_schedules: [
    "index_habit_schedules_habit_id|false|habit_id",
    "index_habit_schedules_habit_effective|false|habit_id,effective_from,effective_until",
  ],
  habit_logs: [
    "index_habit_logs_habit_id_log_date|true|habit_id,log_date",
    "index_habit_logs_habit_date|false|habit_id,log_date",
    "index_habit_logs_log_date|false|log_date",
  ],
  habit_reminders: [
    "index_habit_reminders_habit_id|false|habit_id",
    "index_habit_reminders_habit_id_reminder_time|true|habit_id,reminder_time",
  ],
};

function sameList(label, actual, expected) {
  const a = actual.slice().sort().join(" ; ");
  const b = expected.slice().sort().join(" ; ");
  if (a !== b) { throw new Error(label + " | actual: " + a + " | expected: " + b); }
}

for (const table of Object.keys(expectedColumns)) {
  const entity = schema.database.entities.find((item) => item.tableName === table);
  if (!entity) { throw new Error("table missing from schema: " + table); }

  ok("schema parity: " + table + " columns", () =>
    sameList(table, entity.fields.map((f) =>
      [f.columnName, f.affinity, String(f.notNull), String(f.defaultValue)].join("|")), expectedColumns[table]),
  );

  ok("schema parity: " + table + " indices", () =>
    sameList(table, entity.indices.map((i) =>
      [i.name, String(i.unique), i.columnNames.join(",")].join("|")), expectedIndices[table]),
  );

  ok("schema parity: " + table + " foreign key", () => {
    const actual = JSON.stringify(entity.foreignKeys);
    const expected = table === "habits"
      ? "[]"
      : JSON.stringify([{ table: "habits", onDelete: "CASCADE", onUpdate: "NO ACTION", columns: ["habit_id"], referencedColumns: ["id"] }]);
    if (actual !== expected) { throw new Error("fk | actual: " + actual); }
  });
}

console.log("");
console.log("schema entities: " + schema.database.entities.length + ", triggers installed: " + triggerCount);
console.log("checks: " + checks + ", failed: " + failed);
if (failed > 0) { process.exit(1); }
