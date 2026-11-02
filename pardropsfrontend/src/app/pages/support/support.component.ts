import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { SupportService } from '../../core/services/support.service';
import { SupportConversation, SupportMessage } from '../../core/models/support.model';

/** Página de soporte del usuario. */
@Component({
  selector: 'app-support',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './support.component.html',
  styleUrls: ['./support.component.scss']
})
export class SupportComponent {
  private readonly service = inject(SupportService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  /** Conversaciones del usuario. */
  public readonly conversations = signal<SupportConversation[]>([]);

  /** Conversación abierta. */
  public readonly active = signal<SupportConversation | null>(null);

  /** Mensajes de la conversación abierta. */
  public readonly messages = signal<SupportMessage[]>([]);

  /** Estado de carga. */
  public readonly loading = signal<boolean>(false);

  /** Error mostrado. */
  public readonly error = signal<string | null>(null);

  /** Formulario de nueva conversación. */
  public readonly form = this.fb.nonNullable.group({
    subject: ['', [Validators.required, Validators.maxLength(160)]],
    message: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  /** Mensaje que se está escribiendo. */
  public readonly reply = this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(2000)]);

  /** Temporizador de actualización. */
  private timer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.load();
    this.destroyRef.onDestroy(() => this.stopTimer());
  }

  /** Carga las conversaciones. */
  public load(): void {
    this.loading.set(true);
    this.service.listMine().subscribe({
      next: conversations => {
        this.conversations.set(conversations);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudieron cargar tus conversaciones.');
        this.loading.set(false);
      }
    });
  }

  /** Abre una conversación nueva. */
  public create(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.service.create(value.subject.trim(), value.message.trim()).subscribe({
      next: conversation => {
        this.form.reset();
        this.load();
        this.open(conversation);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudo abrir la conversación.');
      }
    });
  }

  /** Abre una conversación existente. */
  public open(conversation: SupportConversation): void {
    this.active.set(conversation);
    this.loadMessages(conversation.id);
    this.stopTimer();
    this.timer = setInterval(() => this.loadMessages(conversation.id, true), 20000);
  }

  /** Carga los mensajes de la conversación. */
  public loadMessages(id: number, silent = false): void {
    this.service.messages(id).subscribe({
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

  /** Envía la respuesta del usuario. */
  public send(): void {
    const conversation = this.active();
    if (!conversation || this.reply.invalid || !this.reply.value.trim()) {
      return;
    }
    const body = this.reply.value.trim();
    this.service.send(conversation.id, body).subscribe({
      next: () => {
        this.reply.reset();
        this.loadMessages(conversation.id, true);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudo enviar el mensaje.');
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
