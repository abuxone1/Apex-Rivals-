const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_racer_01";
const BOB_UID = "bob_racer_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function serverTs() {
  return new Date();
}

function starterProfile(uid) {
  return {
    userId: uid,
    username: "ApexAlice",
    avatarUrl: "avatar_helmet_red",
    level: 1,
    xp: 0,
    coins: 250,
    credits: 48500,
    totalRaces: 0,
    totalVictories: 0,
    ownedVehicleIds: ["car_gt3_apex", "bike_superbike_1000"],
    unlockedAchievements: [],
    unlockedTrackIds: ["track_neo_tokyo", "track_monza", "track_miami_highway"],
    careerTierUnlocked: 1,
    dailyLoginStreak: 0,
    schemaVersion: 9,
    v8LegacyMigrated: false,
    syncRevision: 1,
    createdAt: serverTs(),
    lastActiveAt: serverTs(),
  };
}

// =========================================================================
// MODULE 2: ZERO-TRUST PROFILE BOUNDS & PRIVILEGED FIELD LOCKDOWN
// =========================================================================
test("Unauthenticated user cannot read or create player profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(starterProfile(ALICE_UID)));
});

test("Authenticated user can create bounded starter profile and update handle, but CANNOT self-grant currencies/level/vehicles or read another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Client attempting to create profile with inflated currency/level MUST fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...starterProfile(ALICE_UID),
      level: 99,
      xp: 999999,
      coins: 500000,
      credits: 99999999,
    })
  );

  // Bounded starter profile creation succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(starterProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Allowed non-privileged update (username + lastActiveAt) succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      username: "ApexAlicePro",
      lastActiveAt: serverTs(),
    })
  );

  // Zero-trust privilege escalation attempts MUST fail
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ credits: 9999999 }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ coins: 50000 }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ xp: 500000, level: 50 }));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ ownedVehicleIds: ["car_f1_hybrid"] })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ unlockedAchievements: ["ach_first_victory"] })
  );
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ isAdmin: true }));

  // Cross-user isolation: Bob cannot read Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

// =========================================================================
// MODULE 3: CLOUD SAVE CONFLICT & IMMUTABLE V8 ROLLBACK BACKUP PROTECTION
// =========================================================================
test("Cloud save enforces monotonic syncRevision, blocks stale overwrites, and protects v8_rollback_backup from deletion/tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdTs = serverTs();
  const saveRef = aliceDb.collection("users").doc(ALICE_UID).collection("cloud_saves").doc("primary");
  const backupRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("cloud_saves")
    .doc("v8_rollback_backup");

  await assertSucceeds(
    saveRef.set({
      userId: ALICE_UID,
      saveSlotId: "primary",
      syncRevision: 5,
      clientDeviceName: "Pixel 9 Pro",
      loadoutsJson: '{"car_gt3":{"engineStage":3}}',
      settingsJson: '{"fps":60}',
      careerProgressJson: '{"tier":2}',
      createdAt: createdTs,
      updatedAt: createdTs,
    })
  );

  // Older offline save (syncRevision: 3) MUST be rejected
  await assertFails(
    saveRef.update({
      syncRevision: 3,
      loadoutsJson: '{"car_gt3":{"engineStage":1}}',
      updatedAt: serverTs(),
    })
  );

  // Newer cloud save (syncRevision: 6) succeeds
  await assertSucceeds(
    saveRef.update({
      syncRevision: 6,
      loadoutsJson: '{"car_gt3":{"engineStage":4}}',
      updatedAt: serverTs(),
    })
  );

  // Seed v8_rollback_backup snapshot; client CANNOT update or delete it
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("cloud_saves")
      .doc("v8_rollback_backup")
      .set({
        userId: ALICE_UID,
        saveSlotId: "v8_rollback_backup",
        syncRevision: 1,
        clientDeviceName: "Server-V8-Migration-Snapshot",
        loadoutsJson: "{}",
        settingsJson: "{}",
        careerProgressJson: '{"v8Credits":92000}',
        createdAt: createdTs,
        updatedAt: createdTs,
      });
  });

  await assertFails(
    backupRef.update({
      syncRevision: 2,
      careerProgressJson: "{}",
      updatedAt: serverTs(),
    })
  );
  await assertFails(backupRef.delete());

  // Listing own cloud save slots succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloud_saves").get()
  );
});

// =========================================================================
// MODULE 4, 5, 7: SERVER-ONLY WRITES FOR LEDGER, MISSIONS, ACHIEVEMENTS, & RACE RESULTS
// =========================================================================
test("Clients can read their own server-written ledger, achievements, events, and race results, but CANNOT forge or self-claim them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = serverTs();

  // Seed authoritative records via Admin context (bypassing rules just like Cloud Functions)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("economy_transactions")
      .doc("tx_ach_master_tuner")
      .set({
        userId: ALICE_UID,
        txType: "ACHIEVEMENT_REWARD",
        idempotencyKey: "tx_ach_master_tuner",
        coinsDelta: 25,
        creditsDelta: 7500,
        balanceAfterCoins: 275,
        balanceAfterCredits: 56000,
        referenceId: "msn_ach_garage_tuner",
        createdAt: ts,
      });

    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("achievements")
      .doc("ach_speed_demon")
      .set({
        userId: ALICE_UID,
        achievementId: "ach_speed_demon",
        title: "330 KM/H Club",
        category: "SPEED",
        progressValue: 335,
        targetValue: 330,
        rewardCredits: 10000,
        rewardCoins: 25,
        rewardXp: 1500,
        unlocked: true,
        claimed: true,
        createdAt: ts,
        updatedAt: ts,
      });

    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("event_completions")
      .doc("msn_daily_apex_hunter")
      .set({
        userId: ALICE_UID,
        eventId: "msn_daily_apex_hunter",
        eventType: "DAILY_MISSION",
        progressValue: 2,
        targetValue: 2,
        rewardCredits: 3500,
        rewardCoins: 15,
        claimed: true,
        createdAt: ts,
        updatedAt: ts,
      });

    await adminDb.collection("race_results").doc("race_verified_01").set({
      userId: ALICE_UID,
      driverName: "ApexAlice",
      trackId: "track_monza",
      trackName: "Monza GP",
      vehicleId: "car_gt3_apex",
      vehicleName: "Apex GT3-R Twin-Turbo",
      gameMode: "TIME_TRIAL",
      finishingPosition: 1,
      raceDurationMs: 162400,
      lapTimeMs: 78420,
      topSpeedKmh: 324.8,
      creditsReward: 6000,
      xpReward: 1200,
      weekBucket: "2026-W46",
      platform: "ANDROID",
      createdAt: ts,
    });
  });

  // Owner reads succeed; cross-user reads fail for private subcollections
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("economy_transactions").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("achievements").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("event_completions").get()
  );
  await assertSucceeds(aliceDb.collection("race_results").doc("race_verified_01").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("economy_transactions").get()
  );

  // Client direct forgery attempts MUST fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("economy_transactions").doc("tx_fake").set({
      userId: ALICE_UID,
      txType: "DAILY_REWARD",
      idempotencyKey: "tx_fake",
      coinsDelta: 9999,
      creditsDelta: 250000,
      balanceAfterCoins: 9999,
      balanceAfterCredits: 250000,
      referenceId: "fake",
      createdAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("achievements").doc("ach_fake").set({
      userId: ALICE_UID,
      achievementId: "ach_fake",
      title: "Fake Achievement",
      category: "CAREER",
      progressValue: 1,
      targetValue: 1,
      rewardCredits: 100000,
      rewardCoins: 1000,
      rewardXp: 25000,
      unlocked: true,
      claimed: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("race_results").doc("race_spoof_01").set({
      userId: ALICE_UID,
      driverName: "ApexAlice",
      trackId: "track_monza",
      trackName: "Monza GP",
      vehicleId: "car_gt3",
      vehicleName: "Apex GT3-R",
      gameMode: "TIME_TRIAL",
      finishingPosition: 1,
      raceDurationMs: 162400,
      lapTimeMs: 15000,
      topSpeedKmh: 320,
      creditsReward: 100000,
      xpReward: 25000,
      weekBucket: "2026-W46",
      platform: "ANDROID",
      createdAt: ts,
    })
  );
});

// =========================================================================
// MODULE 8: MULTIPLAYER ROOM TERMINAL STATE LOCKING
// =========================================================================
test("Multiplayer room locks updates once status reaches FINISHED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const roomRef = aliceDb.collection("multiplayer_rooms").doc("room_alpha_01");
  const createdTs = serverTs();

  await assertSucceeds(
    roomRef.set({
      hostUserId: ALICE_UID,
      roomCode: "APX200",
      trackId: "track_monza",
      vehicleClass: "GT3",
      status: "WAITING",
      maxPlayers: 8,
      playerUids: [ALICE_UID],
      dedicatedServerEndpoint: "wss://eu-west-relay.apexdynamics.gg/room/APX200",
      createdAt: createdTs,
      updatedAt: createdTs,
    })
  );

  // Host transitions room to FINISHED
  await assertSucceeds(
    roomRef.update({
      status: "FINISHED",
      updatedAt: serverTs(),
    })
  );

  // Terminal state lock: cannot reopen or mutate a FINISHED room
  await assertFails(
    roomRef.update({
      status: "WAITING",
      updatedAt: serverTs(),
    })
  );
});
