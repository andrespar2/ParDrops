import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { SupportService } from '../../core/services/support.service';
import { SupportConversation, SupportMessage } from '../../core/models/support.model';

/** Bandeja de soporte para administración. */
@Component({
  selector: 'app-admin-support',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-support.component.html',
  styleUrls: ['../support/support.component.scss']
})
export class AdminSupportComponent {
  private readonly service = inject(SupportService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  /** Conversaciones de todos los usuarios. */
  public readonly conversations = signal<SupportConversation[]>([]);

  /** Conversación abierta. */
  public readonly active = signal<SupportConversation | null>(null);

  /** Mensajes de la conversación abierta. */
  public readonly messages = signal<SupportMessage[]>([]);

  /** Estado de carga. */
  public readonly loading = signal<boolean>(false);

  /** Error mostrado. */
  public readonly error = signal<string | null>(null);

  /** Respuesta que se está escribiendo. */
  public readonly reply = this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(2000)]);

  /** Temporizador de actualización. */
  private timer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.load();
    this.destroyRef.onDestroy(() => this.stopTimer());
  }

  /** Carga la bandeja. */
  public load(): void {
    this.loading.set(true);
    this.service.listForStaff().subscribe({
      next: conversations => {
        this.conversations.set(conversations);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudo cargar la bandeja.');
        this.loading.set(false);
      }
    });
  }

  /** Abre una conversación. */
  public open(conversation: SupportConversation): void {
    this.active.set(conversation);
    this.loadMessages(conversation.id);
    this.stopTimer();
    this.timer = setInterval(() => this.loadMessages(conversation.id, true), 20000);
  }

  /** Carga los mensajes. */
  public loadMessages(id: number, silent = false): void {
    this.service.staffMessages(id).subscribe({
      next: messages => {
        this.messages.set(messages);
        if (!silent) {
          this.load();
        }
      },
      error: (error: HttpErrorResponse) => {
        if (!silent) {
          this.error.set(error.error?.message || 'No se pudieron cargar los mensajes.');
        }
      }
    });
  }

  /** Responde al usuario. */
  public send(): void {
    const conversation = this.active();
    if (!conversation || this.reply.invalid || !this.reply.value.trim()) {
      return;
    }
    this.service.reply(conversation.id, this.reply.value.trim()).subscribe({
      next: () => {
        this.reply.reset();
        this.loadMessages(conversation.id, true);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudo enviar la respuesta.');
      }
    });
  }

  /** Marca la conversación como resuelta. */
  public resolve(): void {
    const conversation = this.active();
    if (!conversation) {
      return;
    }
    this.service.resolve(conversation.id).subscribe({
      next: () => {
        this.active.set({ ...conversation, status: 'RESOLVED' });
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudo resolver la conversación.');
      }
    });
  }

  /** Detiene el temporizador. */
  private stopTimer(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }
}
