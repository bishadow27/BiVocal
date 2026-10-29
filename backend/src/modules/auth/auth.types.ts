export interface UserRow {
  id: number;
  username: string;
  email: string;
  full_name: string | null;
  avatar_url: string | null;
  created_at: Date;
  auth_version?: number;
}

export interface UserCredentialRow extends UserRow {
  password_hash: string | null;
}

export interface GoogleProfile {
  subject: string;
  email: string;
  fullName: string | null;
  pictureUrl: string | null;
  nonce: string;
}

export interface UserDto {
  id: number;
  username: string;
  email: string;
  fullName: string | null;
  avatarUrl: string | null;
}

export interface CurrentUserDto extends UserDto {
  createdAt: string;
}

export interface AuthResponse {
  user: UserDto;
  accessToken: string;
}
