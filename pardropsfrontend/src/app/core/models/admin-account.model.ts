/** Nivel de una cuenta administrativa. */
export type AdminLevel = 'OWNER' | 'MANAGER' | 'SUPPORT';

/** Cuenta administrativa. */
export interface AdminAccount {
  id: number;
  email: string;
  fullName: string;
  level: AdminLevel;
  status: 'ACTIVE' | 'SUSPENDED';
  createdAt: string;
}

/** Datos para crear una cuenta administrativa. */
export interface CreateAdminRequest {
  email: string;
  fullName: string;
  password: string;
  level: Exclude<AdminLevel, 'OWNER'>;
}
