const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      accountNumber: "0123456789",
      accountTag: "@alice",
      ngnBalance: 50000,
      usdBalance: 100,
      displayName: "Alice",
      email: "alice@example.com",
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot update another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      accountNumber: "9876543210",
      accountTag: "@bob",
      ngnBalance: 10000,
      usdBalance: 50,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      ngnBalance: 999999,
    })
  );
});

test("Authenticated user: can create transaction where they are the sender", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("transactions").doc("tx_1").set({
      senderId: ALICE_UID,
      recipientAccount: "9876543210",
      amount: 5000,
      currency: "NGN",
      type: "SEND",
      participants: [ALICE_UID, BOB_UID],
    })
  );
});

test("Authenticated user: cannot read transaction if not in participants", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("transactions").doc("tx_bob_charlie").set({
      senderId: BOB_UID,
      recipientAccount: "1122334455",
      amount: 100,
      currency: "USD",
      type: "SEND",
      participants: [BOB_UID, "charlie_789"],
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("transactions").doc("tx_bob_charlie").get());
});
