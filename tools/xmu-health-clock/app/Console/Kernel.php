<?php

namespace App\Console;

use App\Jobs\HealthClock;
use App\Models\User;
use Illuminate\Console\Scheduling\Schedule;
use Illuminate\Foundation\Console\Kernel as ConsoleKernel;
use Illuminate\Support\Str;

class Kernel extends ConsoleKernel
{
    /**
     * The Artisan commands provided by your application.
     *
     * @var array
     */
    protected $commands = [
        //
    ];

    /**
     * Define the application's command schedule.
     *
     * @param  \Illuminate\Console\Scheduling\Schedule  $schedule
     * @return void
     */
    protected function schedule(Schedule $schedule)
    {
        $schedule->call(function () {
            User::query()->where('auto_clock', '=', true)
                ->get()
                ->shuffle()
                ->each(function (User $user) {
                    $exceptedClockedAt = now()->addMinutes(rand(0, 120))->addSeconds(rand(0, 60));

                    $clock = $user->healthClocks()->create([
                        'id' => Str::orderedUuid()->toString(),
                        'status' => 'pending',
                        'excepted_clocked_at' => $exceptedClockedAt->toString(),
                    ]);

                    HealthClock::dispatch($clock)->delay($exceptedClockedAt);
                });
        })->dailyAt('08:00');
    }

    /**
     * Register the commands for the application.
     *
     * @return void
     */
    protected function commands()
    {
        $this->load(__DIR__.'/Commands');

        require base_path('routes/console.php');
    }
}
