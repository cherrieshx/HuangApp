const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendCommentNotification = onDocumentCreated("recipes/{recipeId}/comments/{commentId}", async (event) => {
    const comment = event.data.data();
    const recipeId = event.params.recipeId;

    try {
        const recipeDoc = await admin.firestore().collection("recipes").doc(recipeId).get();
        if (!recipeDoc.exists) return null;

        const recipeData = recipeDoc.data();
        const authorId = recipeData.userID;
        const recipeName = recipeData.name;

        if (comment.userId === authorId) return null;

        const userDoc = await admin.firestore().collection("users").doc(authorId).get();
        if (!userDoc.exists) return null;

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return null;

        const message = {
            notification: {
                title: "Nuovo commento!",
                body: `${comment.username} ha commentato la tua ricetta: ${recipeName}`
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "CHANNEL_ID_V3",
                    priority: "high",
                    sound: "default"
                }
            },
            data: {
                recipeId: recipeId,
                goToComments: "true",
                type: "NEW_COMMENT"
            },
            token: fcmToken
        };

        return admin.messaging().send(message);
    } catch (error) {
        console.error("Errore commento:", error);
        return null;
    }
});

exports.sendFollowNotification = onDocumentUpdated("users/{userId}", async (event) => {
    const beforeData = event.data.before.data();
    const afterData = event.data.after.data();

    const beforeFollowers = beforeData.followers || [];
    const afterFollowers = afterData.followers || [];

    // Troviamo se è stato aggiunto un nuovo ID alla lista followers
    const newFollowerId = afterFollowers.find(id => !beforeFollowers.includes(id));

    if (newFollowerId) {
        try {
            const followerDoc = await admin.firestore().collection("users").doc(newFollowerId).get();
            const followerName = followerDoc.exists ? (followerDoc.data().username || "Qualcuno") : "Qualcuno";

            // Il token appartiene all'utente che è stato seguito (userId nel percorso)
            const fcmToken = afterData.fcmToken;
            if (!fcmToken) return null;

            const message = {
                notification: {
                    title: "Nuovo Follower! 👤",
                    body: `${followerName} ha iniziato a seguirti.`
                },
                android: {
                    priority: "high",
                    notification: {
                        channelId: "CHANNEL_ID_V3",
                        priority: "high",
                        sound: "default"
                    }
                },
                data: {
                    userId: newFollowerId, // ID di chi ha fatto il follow per aprire il suo profilo
                    type: "NEW_FOLLOW"
                },
                token: fcmToken
            };

            return admin.messaging().send(message);
        } catch (error) {
            console.error("Errore follow:", error);
        }
    }
    return null;
});