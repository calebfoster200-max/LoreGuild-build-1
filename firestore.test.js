const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";
const CHARLIE_UID = "charlie_uid_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("servers").where("visibility", "==", "public").get());
  await assertFails(unauth.collection("server_invites").doc("EMBER7").get());
});

test("User profile and characters: owner CRUD succeeds, cross-user access denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime();

  // Create user profile (optional fields omitted via filterValues)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Create character
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("characters").doc("char_1").set({
      userId: ALICE_UID,
      name: "Kaelen Voss",
      race: "Half-Elf",
      characterClass: "Spellblade",
      title: "Warden of Ash",
      level: 5,
      alignment: "Chaotic Good",
      biography: "Former spire sentinel seeking the lost lantern.",
      avatarKey: "knight",
      accentColorHex: "#E5B869",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update on character preserves immutable createdAt & userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("characters").doc("char_1").update({
      name: "Kaelen the Unbroken",
      level: 6,
      updatedAt: pastTime(1000),
    })
  );

  // List own characters succeeds (empty or populated)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("characters").get()
  );

  // Update activeCharacterId on user profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeCharacterId: "char_1",
      updatedAt: pastTime(500),
    })
  );

  // Bob cannot read or write Alice's profile or characters
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("characters").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("characters").doc("char_2").set({
      userId: BOB_UID,
      name: "Intruder",
      race: "Human",
      characterClass: "Rogue",
      title: "",
      level: 1,
      alignment: "Neutral",
      biography: "",
      avatarKey: "mage",
      accentColorHex: "#6EC6B8",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Shadow update & invalid fields rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("characters").doc("char_bad").set({
      id: "char_bad",
      userId: ALICE_UID,
      name: "Bad",
      race: "Elf",
      characterClass: "Mage",
      title: "",
      level: 1,
      alignment: "Neutral",
      biography: "",
      avatarKey: "mage",
      accentColorHex: "#6EC6B8",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("characters").doc("char_future").set({
      userId: ALICE_UID,
      name: "Time Traveler",
      race: "Elf",
      characterClass: "Mage",
      title: "",
      level: 1,
      alignment: "Neutral",
      biography: "",
      avatarKey: "mage",
      accentColorHex: "#6EC6B8",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: ts,
    })
  );
});

test("Public server creation, discovery, join, character switch, and real-time chat", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTime();

  // Alice atomically creates public server + owner member + invite lookup
  const batch = aliceDb.batch();
  const serverRef = aliceDb.collection("servers").doc("srv_tavern");
  const ownerMemberRef = serverRef.collection("members").doc(ALICE_UID);
  const inviteRef = aliceDb.collection("server_invites").doc("TAVERN1");

  batch.set(serverRef, {
    ownerId: ALICE_UID,
    ownerName: "Alice",
    name: "The Lantern & Quill",
    genre: "High Fantasy",
    description: "A warm hearth for wayward travelers.",
    visibility: "public",
    inviteCode: "TAVERN1",
    bannerKey: "tavern",
    memberCount: 1,
    memberUids: [ALICE_UID],
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(ownerMemberRef, {
    userId: ALICE_UID,
    playerName: "Alice",
    characterId: "char_1",
    characterName: "Kaelen Voss",
    characterRace: "Half-Elf",
    characterClass: "Spellblade",
    characterTitle: "Warden",
    characterAvatarKey: "knight",
    characterAccentHex: "#E5B869",
    role: "owner",
    joinedAt: ts,
    updatedAt: ts,
  });
  batch.set(inviteRef, {
    inviteCode: "TAVERN1",
    serverId: "srv_tavern",
    serverName: "The Lantern & Quill",
    genre: "High Fantasy",
    visibility: "public",
    ownerId: ALICE_UID,
    createdAt: ts,
  });
  await assertSucceeds(batch.commit());

  // Bob discovers public servers via filtered query (unfiltered query fails)
  await assertSucceeds(bobDb.collection("servers").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("servers").get());

  // Non-member Charlie cannot read messages before joining
  await assertFails(charlieDb.collection("servers").doc("srv_tavern").collection("messages").get());

  // Bob joins public server with his selected character in atomic batch
  const bobJoinBatch = bobDb.batch();
  bobJoinBatch.set(bobDb.collection("servers").doc("srv_tavern").collection("members").doc(BOB_UID), {
    userId: BOB_UID,
    playerName: "Bob",
    characterId: "bob_char_1",
    characterName: "Lyra Vance",
    characterRace: "Tiefling",
    characterClass: "Chronomancer",
    characterTitle: "Star Weaver",
    characterAvatarKey: "mage",
    characterAccentHex: "#6EC6B8",
    role: "member",
    joinedWithCode: "PUBLIC",
    joinedAt: pastTime(2000),
    updatedAt: pastTime(2000),
  });
  bobJoinBatch.update(bobDb.collection("servers").doc("srv_tavern"), {
    memberUids: [ALICE_UID, BOB_UID],
    memberCount: 2,
    updatedAt: pastTime(2000),
  });
  await assertSucceeds(bobJoinBatch.commit());

  // Bob queries his joined servers
  await assertSucceeds(
    bobDb.collection("servers").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Bob posts dialogue & action messages; Alice reads messages & roster
  await assertSucceeds(
    bobDb.collection("servers").doc("srv_tavern").collection("messages").doc("msg_1").set({
      serverId: "srv_tavern",
      senderUid: BOB_UID,
      characterId: "bob_char_1",
      characterName: "Lyra Vance",
      characterRace: "Tiefling",
      characterClass: "Chronomancer",
      characterAvatarKey: "mage",
      characterAccentHex: "#6EC6B8",
      messageType: "dialogue",
      content: "Evening, traveler. The stars foretold your arrival.",
      createdAt: pastTime(1000),
    })
  );
  await assertSucceeds(aliceDb.collection("servers").doc("srv_tavern").collection("messages").get());
  await assertSucceeds(aliceDb.collection("servers").doc("srv_tavern").collection("members").get());

  // Bob switches character inside the server
  await assertSucceeds(
    bobDb.collection("servers").doc("srv_tavern").collection("members").doc(BOB_UID).update({
      characterId: "bob_char_2",
      characterName: "Garrett Stone",
      characterRace: "Dwarf",
      characterClass: "Artificer",
      characterTitle: "Runesmith",
      characterAvatarKey: "crest_ember",
      characterAccentHex: "#D9777F",
      updatedAt: pastTime(500),
    })
  );

  // Bob cannot spoof Alice's senderUid in a message
  await assertFails(
    bobDb.collection("servers").doc("srv_tavern").collection("messages").doc("msg_spoof").set({
      serverId: "srv_tavern",
      senderUid: ALICE_UID,
      characterId: "bob_char_2",
      characterName: "Garrett",
      characterRace: "Dwarf",
      characterClass: "Artificer",
      characterAvatarKey: "crest_ember",
      characterAccentHex: "#D9777F",
      messageType: "dialogue",
      content: "Spoofed!",
      createdAt: pastTime(200),
    })
  );
});

test("Private server invite gate: requires exact invite code, rejects guessing and listing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTime();

  const batch = aliceDb.batch();
  const serverRef = aliceDb.collection("servers").doc("srv_secret");
  batch.set(serverRef, {
    ownerId: ALICE_UID,
    ownerName: "Alice",
    name: "Obsidian Conclave",
    genre: "Gothic Mystery",
    description: "Invite-only investigation table.",
    visibility: "private",
    inviteCode: "SECRET9",
    bannerKey: "citadel",
    memberCount: 1,
    memberUids: [ALICE_UID],
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(serverRef.collection("members").doc(ALICE_UID), {
    userId: ALICE_UID,
    playerName: "Alice",
    characterId: "char_1",
    characterName: "Kaelen Voss",
    characterRace: "Half-Elf",
    characterClass: "Spellblade",
    characterTitle: "Warden",
    characterAvatarKey: "knight",
    characterAccentHex: "#E5B869",
    role: "owner",
    joinedAt: ts,
    updatedAt: ts,
  });
  batch.set(aliceDb.collection("server_invites").doc("SECRET9"), {
    inviteCode: "SECRET9",
    serverId: "srv_secret",
    serverName: "Obsidian Conclave",
    genre: "Gothic Mystery",
    visibility: "private",
    ownerId: ALICE_UID,
    createdAt: ts,
  });
  await assertSucceeds(batch.commit());

  // Cannot list server_invites
  await assertFails(bobDb.collection("server_invites").get());

  // Non-member cannot get private server directly
  await assertFails(bobDb.collection("servers").doc("srv_secret").get());

  // Charlie tries joining with WRONG invite code -> rejected
  const badJoin = charlieDb.batch();
  badJoin.set(charlieDb.collection("servers").doc("srv_secret").collection("members").doc(CHARLIE_UID), {
    userId: CHARLIE_UID,
    playerName: "Charlie",
    characterId: "c1",
    characterName: "Cinder",
    characterRace: "Elf",
    characterClass: "Rogue",
    characterTitle: "",
    characterAvatarKey: "crest_moon",
    characterAccentHex: "#9D8DF1",
    role: "member",
    joinedWithCode: "WRONGCODE",
    joinedAt: pastTime(1000),
    updatedAt: pastTime(1000),
  });
  badJoin.update(charlieDb.collection("servers").doc("srv_secret"), {
    memberUids: [ALICE_UID, CHARLIE_UID],
    memberCount: 2,
    updatedAt: pastTime(1000),
  });
  await assertFails(badJoin.commit());

  // Bob looks up exact invite code, then joins with matching code -> succeeds
  await assertSucceeds(bobDb.collection("server_invites").doc("SECRET9").get());
  const goodJoin = bobDb.batch();
  goodJoin.set(bobDb.collection("servers").doc("srv_secret").collection("members").doc(BOB_UID), {
    userId: BOB_UID,
    playerName: "Bob",
    characterId: "b1",
    characterName: "Lyra Vance",
    characterRace: "Tiefling",
    characterClass: "Chronomancer",
    characterTitle: "",
    characterAvatarKey: "mage",
    characterAccentHex: "#6EC6B8",
    role: "member",
    joinedWithCode: "SECRET9",
    joinedAt: pastTime(1000),
    updatedAt: pastTime(1000),
  });
  goodJoin.update(bobDb.collection("servers").doc("srv_secret"), {
    memberUids: [ALICE_UID, BOB_UID],
    memberCount: 2,
    updatedAt: pastTime(1000),
  });
  await assertSucceeds(goodJoin.commit());
});
