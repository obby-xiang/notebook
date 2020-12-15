<?php

namespace App\Jobs;

use GuzzleHttp\Exception\GuzzleException;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Throwable;

class HealthClock implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public $healthClock;

    /**
     * Create a new job instance.
     *
     * @param  \App\Models\HealthClock  $healthClock  health clock
     */
    public function __construct(\App\Models\HealthClock $healthClock)
    {
        $this->healthClock = $healthClock;
    }

    /**
     * Execute the job.
     *
     * @return void
     * @throws GuzzleException exception
     */
    public function handle()
    {
        $clock = new \App\Support\HealthClock($this->healthClock->user, true);

        $clock->login();
        $clock->clock();

        $this->healthClock->update([
            'status' => 'success',
            'message' => 'Clock success.',
            'clocked_at' => now()->toString(),
        ]);

        $this->healthClock->user->notify(new \App\Notifications\HealthClock($this->healthClock));
    }

    /**
     * Job failed.
     *
     * @param  Throwable  $exception  exception
     */
    public function failed(Throwable $exception)
    {
        $this->healthClock->update([
            'status' => 'failed',
            'message' => "Clock failed.\n{$exception->getMessage()}",
            'clocked_at' => now()->toString(),
        ]);

        $this->healthClock->user->notify(new \App\Notifications\HealthClock($this->healthClock));
    }
}
