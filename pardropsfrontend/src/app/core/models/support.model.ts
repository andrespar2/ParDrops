/** Estado de una conversación de soporte. */
export type SupportStatus = 'OPEN' | 'RESOLVED';

/** Resumen de una conversación. */
export interface SupportConversation {
  id: number;
  subject: string;
  status: SupportStatus;
  lastMessage: string;
  lastMessageAt: string;
  unread: boolean;
  createdAt: string;
}

/** Mensaje de una conversación. */
export interface SupportMessage {
  id: number;
  body: string;
  fromStaff: boolean;
  authorName: string;
  createdAt: string;
}
