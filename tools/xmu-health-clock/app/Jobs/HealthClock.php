<?php

namespace App\Jobs;

use App\Models\User;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;
use Throwable;

class HealthClock implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    protected $user;

    /**
     * Create a new job instance.
     *
     * @param  User  $user
     */
    public function __construct(User $user)
    {
        $this->user = $user;
    }

    /**
     * Execute the job.
     *
     * @return void
     */
    public function handle()
    {
        $status = 'failed';

        try {
            $healthClock = new \App\Support\HealthClock($this->user, true);

            if ($healthClock->login() && $healthClock->clock()) {
                $status = 'success';
            }
        } catch (Throwable $e) {
            Log::error($e);
        }

        if (!is_null($this->user->email)) {
            $this->user->notify(new \App\Notifications\HealthClock($status));
        }
    }
}
