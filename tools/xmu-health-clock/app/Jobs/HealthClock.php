<?php

namespace App\Jobs;

use App\Models\User;
use GuzzleHttp\Client;
use GuzzleHttp\RequestOptions;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldBeUnique;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Crypt;
use Symfony\Component\DomCrawler\Crawler;

class HealthClock implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    protected $user;

    protected $client;

    /**
     * Create a new job instance.
     *
     * @param User $user
     */
    public function __construct(User $user)
    {
        $this->user = $user;

        $this->client = new Client([
            RequestOptions::ALLOW_REDIRECTS => [
                'referer' => true,
                'track_redirects' => true,
            ],
            RequestOptions::COOKIES => true,
            RequestOptions::VERIFY => false,
            RequestOptions::HEADERS => [
                'user-agent' => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36',
            ],
        ]);
    }

    /**
     * Execute the job.
     *
     * @return void
     */
    public function handle()
    {
        $this->login();
        $this->healthClock();
    }

    protected function login()
    {
        $form = (new Crawler(
            $this->client->get('http://ids.xmu.edu.cn/authserver/login?service=https://xmuxg.xmu.edu.cn/login/cas/xmu')
                ->getBody()->getContents(),
            'http://ids.xmu.edu.cn'
        ))->filter('body #casLoginForm')->form()->getValues();

        $form['username'] = $this->user->username;
        $form['password'] = Crypt::decryptString($this->user->password);

        $this->client->post('https://ids.xmu.edu.cn/authserver/login?service=https://xmuxg.xmu.edu.cn/login/cas/xmu', [
            RequestOptions::HEADERS => [
                'host' => 'ids.xmu.edu.cn',
                'origin' => 'https://ids.xmu.edu.cn',
                'referer' => 'https://ids.xmu.edu.cn/authserver/login?service=https://xmuxg.xmu.edu.cn/login/cas/xmu',
            ],
            RequestOptions::FORM_PARAMS => $form,
        ])->getBody()->getContents();
    }

    protected function healthClock()
    {
        $business = json_decode($this->client->get('https://xmuxg.xmu.edu.cn/api/app/214/business/now?getFirst=true', [
            RequestOptions::HEADERS => [
                'referer' => 'https://xmuxg.xmu.edu.cn/app/214',
            ],
        ])->getBody()->getContents())->data[0]->business;

        $ownerNode = collect($business->businessTimeList)->filter(function ($item) {
            return $item->nodeId === 'owner';
        })->first();

        if ($ownerNode->startDate == null || Carbon::parse($ownerNode->startDate)->isFuture()) {
            return;
        }

        $deadline = $ownerNode->endDate ?? $business->endTime;

        if ($deadline == null || Carbon::parse($deadline)->isPast()) {
            return;
        }

        $instance = json_decode($this->client->get("https://xmuxg.xmu.edu.cn/api/formEngine/business/{$business->id}/myFormInstance", [
            RequestOptions::HEADERS => [
                'referer' => 'https://xmuxg.xmu.edu.cn/app/214',
            ],
        ])->getBody()->getContents())->data;

        $fills = [
            'select_1584240106785' => ['stringValue' => '是'], // 学生本人是否填写
            'select_1582538939790' => ['stringValue' => '是 Yes'], // 本人是否承诺所填报的全部内容均属实、准确，不存在任何隐瞒和不实的情况，更无遗漏之处
        ];

        $formData = collect($instance->formData)->filter(function ($item) use ($fills) {
            return in_array($item->name, array_keys($fills));
        })->map(function ($item) use ($fills) {
            $item->value = $fills[$item->name];

            return $item;
        })->values()->toArray();

        $this->client->post("https://xmuxg.xmu.edu.cn/api/formEngine/formInstance/{$instance->id}", [
            RequestOptions::HEADERS => [
                'origin' => 'https://xmuxg.xmu.edu.cn',
                'referer' => 'https://xmuxg.xmu.edu.cn/app/214',
            ],
            RequestOptions::JSON => [
                'formData' => $formData,
                'playerId' => 'owner',
            ],
        ])->getBody()->getContents();
    }
}
