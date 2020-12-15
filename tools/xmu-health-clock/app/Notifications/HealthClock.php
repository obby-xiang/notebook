<?php

namespace App\Notifications;

use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;

class HealthClock extends Notification implements ShouldQueue
{
    use Queueable;

    protected $healthClock;

    /**
     * Create a new notification instance.
     *
     * @param  \App\Models\HealthClock  $healthClock  healthClock
     */
    public function __construct(\App\Models\HealthClock $healthClock)
    {
        $this->healthClock = $healthClock;
    }

    /**
     * Get the notification's delivery channels.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function via($notifiable)
    {
        return ['mail'];
    }

    /**
     * Get the mail representation of the notification.
     *
     * @param  mixed  $notifiable
     * @return MailMessage
     */
    public function toMail($notifiable)
    {
        return (new MailMessage)
            ->subject("Clocked {$this->healthClock->status}.")
            ->line($this->healthClock->message);
    }

    /**
     * Get the array representation of the notification.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function toArray($notifiable)
    {
        return [
            //
        ];
    }
}
