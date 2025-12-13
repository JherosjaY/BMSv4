import * as admin from "firebase-admin";

/**
 * Firebase Cloud Messaging Service
 * Handles sending push notifications to Android devices
 * 
 * SECURITY: Uses environment variables instead of files
 * - FIREBASE_SERVICE_ACCOUNT_JSON: Full service account JSON as env var
 * - FIREBASE_PROJECT_ID: Firebase project ID
 */
export class FCMService {
  private app: admin.app.App | null = null;
  private messaging: admin.messaging.Messaging | null = null;
  private isEnabled: boolean = false;

  constructor() {
    try {
      // Get Firebase credentials from environment variable (SECURE)
      const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
      const projectId = process.env.FIREBASE_PROJECT_ID || "blotter-management-syste-5ee0f";

      // If Firebase is not configured, skip initialization (optional for school projects)
      if (!serviceAccountJson) {
        console.warn("⚠️ FIREBASE_SERVICE_ACCOUNT_JSON environment variable not set");
        console.warn("⚠️ FCM notifications disabled - app will work without push notifications");
        this.isEnabled = false;
        return;
      }

      // Parse the JSON string from environment variable
      let serviceAccount: any;
      try {
        serviceAccount = JSON.parse(serviceAccountJson);
      } catch (parseError: any) {
        console.error("❌ Invalid FIREBASE_SERVICE_ACCOUNT_JSON format");
        console.error("❌ Ensure the JSON is properly formatted and escaped");
        throw new Error("Failed to parse Firebase service account JSON: " + parseError.message);
      }

      // Validate required fields in service account
      if (!serviceAccount.type || !serviceAccount.project_id || !serviceAccount.private_key) {
        console.error("❌ Firebase service account JSON is missing required fields");
        console.error("❌ Required: type, project_id, private_key");
        throw new Error("Invalid Firebase service account structure");
      }

      // Initialize Firebase Admin SDK with credentials
      this.app = admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        projectId: projectId,
      });

      this.messaging = admin.messaging(this.app);
      this.isEnabled = true;
      console.log("✅ Firebase Admin SDK initialized for FCM");
      console.log(`✅ Using Firebase project: ${projectId}`);
    } catch (error: any) {
      console.error("❌ Error initializing Firebase Admin SDK:", error.message);
      console.warn("⚠️ FCM notifications will be disabled");
      this.isEnabled = false;
    }
  }

  /**
   * Send push notification to a single device
   * @param fcmToken - FCM token of the device
   * @param title - Notification title
   * @param body - Notification body/message
   * @param data - Additional data to send
   */
  async sendNotification(
    fcmToken: string,
    title: string,
    body: string,
    data?: Record<string, string>
  ): Promise<string> {
    try {
      // If Firebase is not enabled, skip sending
      if (!this.isEnabled) {
        console.warn("⚠️ FCM is not enabled - notification not sent");
        return "FCM_DISABLED";
      }

      if (!fcmToken) {
        throw new Error("FCM token is required");
      }

      const message: admin.messaging.Message = {
        notification: {
          title: title,
          body: body,
        },
        data: data || {},
        token: fcmToken,
      };

      const messageId = await this.messaging!.send(message);
      console.log(`✅ Push notification sent to ${fcmToken.substring(0, 20)}...`);
      console.log(`   Message ID: ${messageId}`);

      return messageId;
    } catch (error: any) {
      console.error("❌ Error sending push notification:", error.message);
      throw error;
    }
  }

  /**
   * Send push notification to multiple devices
   * @param fcmTokens - Array of FCM tokens
   * @param title - Notification title
   * @param body - Notification body/message
   * @param data - Additional data to send
   */
  async sendMulticastNotification(
    fcmTokens: string[],
    title: string,
    body: string,
    data?: Record<string, string>
  ): Promise<admin.messaging.BatchResponse> {
    try {
      if (!fcmTokens || fcmTokens.length === 0) {
        throw new Error("At least one FCM token is required");
      }

      const message: admin.messaging.MulticastMessage = {
        notification: {
          title: title,
          body: body,
        },
        data: data || {},
        tokens: fcmTokens,
      };

      const response = await this.messaging.sendMulticast(message);
      console.log(`✅ Multicast notification sent to ${response.successCount} devices`);
      console.log(`⚠️ Failed: ${response.failureCount} devices`);

      return response;
    } catch (error: any) {
      console.error("❌ Error sending multicast notification:", error.message);
      throw error;
    }
  }

  /**
   * Send notification with custom data payload
   * @param fcmToken - FCM token
   * @param title - Title
   * @param body - Body
   * @param customData - Custom data object
   */
  async sendDataNotification(
    fcmToken: string,
    title: string,
    body: string,
    customData: Record<string, string>
  ): Promise<string> {
    try {
      const message: admin.messaging.Message = {
        notification: {
          title: title,
          body: body,
        },
        data: {
          ...customData,
          timestamp: new Date().toISOString(),
        },
        token: fcmToken,
        android: {
          priority: "high",
          notification: {
            sound: "default",
            channelId: "default",
          },
        },
      };

      const messageId = await this.messaging.send(message);
      console.log(`✅ Data notification sent: ${messageId}`);

      return messageId;
    } catch (error: any) {
      console.error("❌ Error sending data notification:", error.message);
      throw error;
    }
  }

  /**
   * Send notification to a topic
   * @param topic - Topic name
   * @param title - Notification title
   * @param body - Notification body
   */
  async sendTopicNotification(
    topic: string,
    title: string,
    body: string
  ): Promise<string> {
    try {
      const message: admin.messaging.Message = {
        notification: {
          title: title,
          body: body,
        },
        topic: topic,
      };

      const messageId = await this.messaging.send(message);
      console.log(`✅ Topic notification sent to '${topic}': ${messageId}`);

      return messageId;
    } catch (error: any) {
      console.error("❌ Error sending topic notification:", error.message);
      throw error;
    }
  }

  /**
   * Subscribe device to topic
   * @param fcmToken - FCM token
   * @param topic - Topic name
   */
  async subscribeToTopic(fcmToken: string, topic: string): Promise<void> {
    try {
      await this.messaging.subscribeToTopic([fcmToken], topic);
      console.log(`✅ Device subscribed to topic: ${topic}`);
    } catch (error: any) {
      console.error("❌ Error subscribing to topic:", error.message);
      throw error;
    }
  }

  /**
   * Unsubscribe device from topic
   * @param fcmToken - FCM token
   * @param topic - Topic name
   */
  async unsubscribeFromTopic(fcmToken: string, topic: string): Promise<void> {
    try {
      await this.messaging.unsubscribeFromTopic([fcmToken], topic);
      console.log(`✅ Device unsubscribed from topic: ${topic}`);
    } catch (error: any) {
      console.error("❌ Error unsubscribing from topic:", error.message);
      throw error;
    }
  }
}

// Export singleton instance
export const fcmService = new FCMService();
