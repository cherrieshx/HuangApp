const { onDocumentCreated } = require("firebase-functions/v2/firestore");
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

        const response = await admin.messaging().send(message);
        console.log("Notifica inviata:", response);
        return response;

    } catch (error) {
        console.error("Errore:", error);
        return null;
    }
});