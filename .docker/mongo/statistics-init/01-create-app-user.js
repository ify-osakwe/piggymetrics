// Creates the application user for the 'statistics' database
// Requires env vars: MONGODB_APP_USER, MONGODB_APP_PASSWORD

const dbName = 'statistics'; // must match MONGO_INITDB_DATABASE
const appUser = process.env.MONGODB_APP_USER;
const appPass = process.env.MONGODB_APP_PASSWORD;

if (!appUser || !appPass) {
    throw new Error('MONGODB_APP_USER / MONGODB_APP_PASSWORD not set');
}

const database = db.getSiblingDB(dbName);
database.createUser({
    user: appUser,
    pwd: appPass,
    roles: [{ role: 'readWrite', db: dbName }],
});

