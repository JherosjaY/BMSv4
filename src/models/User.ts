export interface User {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  role: "User" | "Officer" | "Admin";
  badgeNumber?: string;
  profilePhotoUri?: string;
  isActive: boolean;
  profileCompleted: boolean;
  mustChangePassword: boolean;
  fcmToken?: string;
  deviceId?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  role?: "User" | "Officer" | "Admin";
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  isActive?: boolean;
  profilePhotoUri?: string;
}

export interface UserResponse {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  role: string;
  profilePhotoUri?: string;
  profileCompleted: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  message: string;
  data: {
    user: UserResponse;
    token: string;
  };
}

export interface UpdateProfileRequest {
  profilePhotoUri: string;
  profileCompleted?: boolean;
}

export interface SaveFCMTokenRequest {
  userId: number;
  fcmToken: string;
  deviceId?: string;
}
