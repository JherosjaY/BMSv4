import sgMail from "@sendgrid/mail";

// Email service for sending verification codes
export class EmailService {
  constructor() {
    sgMail.setApiKey(process.env.SENDGRID_API_KEY || "");
  }

  /**
   * Generate a random 6-digit verification code
   */
  generateVerificationCode(): string {
    return Math.floor(100000 + Math.random() * 900000).toString();
  }

  /**
   * Send verification code email using SendGrid
   */
  async sendVerificationEmail(
    email: string,
    code: string,
    username: string
  ): Promise<boolean> {
    try {
      const msg = {
        to: email,
        from: process.env.SENDGRID_FROM || "official.bms.2025@gmail.com",
        subject: "BMS Account Verification Code",
        html: `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px;">
              <h2 style="color: #333; margin-bottom: 20px;">Email Verification</h2>
              <p style="color: #666; font-size: 16px; margin-bottom: 20px;">
                Hello <strong>${username}</strong>,
              </p>
              <p style="color: #666; font-size: 16px; margin-bottom: 30px;">
                Thank you for registering with Blotter Management System. Please use the verification code below to complete your registration:
              </p>
              <div style="background-color: #fff; padding: 20px; border-radius: 8px; text-align: center; margin-bottom: 30px;">
                <p style="color: #333; font-size: 14px; margin-bottom: 10px;">Your Verification Code:</p>
                <p style="color: #007bff; font-size: 32px; font-weight: bold; letter-spacing: 5px; margin: 0;">
                  ${code}
                </p>
              </div>
              <p style="color: #666; font-size: 14px; margin-bottom: 10px;">
                This code will expire in <strong>10 minutes</strong>.
              </p>
              <p style="color: #666; font-size: 14px; margin-bottom: 20px;">
                If you did not request this code, please ignore this email.
              </p>
              <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
              <p style="color: #999; font-size: 12px; text-align: center;">
                © 2025 Blotter Management System. All rights reserved.
              </p>
            </div>
          </div>
        `,
      };
      await sgMail.send(msg);
      console.log("✅ Email sent via SendGrid");
      return true;
    } catch (error) {
      console.error("❌ Error sending email via SendGrid:", error);
      return false;
    }
  }

  /**
   * Send password reset email
   */
  async sendPasswordResetEmail(
    email: string,
    resetCode: string,
    username: string
  ): Promise<boolean> {
    try {
      const mailOptions = {
        from: process.env.GMAIL_USER,
        to: email,
        subject: "BMS Password Reset Request",
        html: `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px;">
              <h2 style="color: #333; margin-bottom: 20px;">Password Reset Request</h2>
              
              <p style="color: #666; font-size: 16px; margin-bottom: 20px;">
                Hello <strong>${username}</strong>,
              </p>
              
              <p style="color: #666; font-size: 16px; margin-bottom: 30px;">
                We received a request to reset your password. Use the code below to reset your password:
              </p>
              
              <div style="background-color: #fff; padding: 20px; border-radius: 8px; text-align: center; margin-bottom: 30px;">
                <p style="color: #333; font-size: 14px; margin-bottom: 10px;">Your Reset Code:</p>
                <p style="color: #dc3545; font-size: 32px; font-weight: bold; letter-spacing: 5px; margin: 0;">
                  ${resetCode}
                </p>
              </div>
              
              <p style="color: #666; font-size: 14px; margin-bottom: 10px;">
                This code will expire in <strong>1 hour</strong>.
              </p>
              
              <p style="color: #666; font-size: 14px; margin-bottom: 20px;">
                If you did not request this, please ignore this email.
              </p>
              
              <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
              
              <p style="color: #999; font-size: 12px; text-align: center;">
                © 2025 Blotter Management System. All rights reserved.
              </p>
            </div>
          </div>
        `,
      };

      const info = await this.transporter.sendMail(mailOptions);
      console.log("✅ Password reset email sent:", info.response);
      return true;
    } catch (error) {
      console.error("❌ Error sending password reset email:", error);
      return false;
    }
  }

  /**
   * Send officer credentials email
   */
  async sendOfficerCredentialsEmail(
    email: string,
    officerName: string,
    username: string,
    password: string
  ): Promise<boolean> {
    try {
      const mailOptions = {
        from: process.env.GMAIL_USER,
        to: email,
        subject: "BMS Officer Account Credentials",
        html: `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px;">
              <h2 style="color: #333; margin-bottom: 20px;">Officer Account Created</h2>
              
              <p style="color: #666; font-size: 16px; margin-bottom: 20px;">
                Hello <strong>${officerName}</strong>,
              </p>
              
              <p style="color: #666; font-size: 16px; margin-bottom: 30px;">
                Your officer account has been successfully created in the Blotter Management System. Please use the credentials below to log in:
              </p>
              
              <div style="background-color: #fff; padding: 20px; border-radius: 8px; margin-bottom: 30px; border-left: 4px solid #007bff;">
                <p style="color: #333; font-size: 14px; margin-bottom: 15px;"><strong>Login Credentials:</strong></p>
                
                <p style="color: #666; font-size: 14px; margin-bottom: 10px;">
                  <strong>Username:</strong> <code style="background-color: #f5f5f5; padding: 5px 10px; border-radius: 4px; font-family: monospace;">${username}</code>
                </p>
                
                <p style="color: #666; font-size: 14px; margin-bottom: 10px;">
                  <strong>Password:</strong> <code style="background-color: #f5f5f5; padding: 5px 10px; border-radius: 4px; font-family: monospace;">${password}</code>
                </p>
              </div>
              
              <p style="color: #dc3545; font-size: 14px; margin-bottom: 20px;">
                ⚠️ <strong>Important:</strong> Please change your password after your first login for security purposes.
              </p>
              
              <p style="color: #666; font-size: 14px; margin-bottom: 20px;">
                If you have any questions or need assistance, please contact the system administrator.
              </p>
              
              <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
              
              <p style="color: #999; font-size: 12px; text-align: center;">
                © 2025 Blotter Management System. All rights reserved.
              </p>
            </div>
          </div>
        `,
      };

      const info = await this.transporter.sendMail(mailOptions);
      console.log("✅ Officer credentials email sent:", info.response);
      return true;
    } catch (error) {
      console.error("❌ Error sending officer credentials email:", error);
      return false;
    }
  }
}

// Export singleton instance
export const emailService = new EmailService();
