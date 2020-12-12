<?php

namespace App\Support;

use App\Models\User;
use GuzzleHttp\Client;
use GuzzleHttp\Cookie\CookieJar;
use GuzzleHttp\Exception\GuzzleException;
use GuzzleHttp\RequestOptions;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Crypt;
use Illuminate\Support\Str;
use Symfony\Component\DomCrawler\Crawler;

class HealthClock
{
    protected $user;

    protected $standalone;

    protected $cookies;

    protected $client;

    /**
     * HealthClock constructor.
     *
     * @param  User  $user  user
     * @param  bool  $standalone  standalone
     */
    public function __construct(User $user, bool $standalone = false)
    {
        $this->user = $user;

        $this->standalone = $standalone;

        if ($this->standalone || is_null($this->user->cookie)) {
            $this->cookies = new CookieJar;
        } else {
            $this->cookies = new CookieJar(false, json_decode($this->user->cookie, true));
        }

        $this->client = new Client([
            RequestOptions::ALLOW_REDIRECTS => [
                'referer' => true,
                'track_redirects' => true,
            ],
            RequestOptions::COOKIES => $this->cookies,
            RequestOptions::VERIFY => false,
            RequestOptions::HEADERS => [
                'cache-control' => 'no-cache',
                'user-agent' => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36',
            ],
        ]);
    }

    /**
     * Login.
     *
     * @return bool true if success, otherwise false
     * @throws GuzzleException exception
     */
    public function login(): bool
    {
        if ($this->authed()) {
            return true;
        }

        $form = (new Crawler(
            $this->client->get('http://ids.xmu.edu.cn/authserver/login?service=https://xmuxg.xmu.edu.cn/login/cas/xmu')
                ->getBody()->getContents(),
            'http://ids.xmu.edu.cn'
        ))->filter('body #casLoginForm')->form()->getValues();

        $form['username'] = $this->user->username;
        $form['password'] = Crypt::decryptString($this->user->password);

        $timestamp = now()->timestamp.now()->milliseconds;

        if ($this->client->get("https://ids.xmu.edu.cn/authserver/needCaptcha.html?username={$form['username']}&_={$timestamp}",
                [
                    RequestOptions::HEADERS => [
                        'referer' => 'https://ids.xmu.edu.cn/authserver/login?service=https://xmuxg.xmu.edu.cn/login/cas/xmu',
                    ],
                ]
            )->getBody()->getContents() === 'true') {
            return false;
        }

        $this->client->post('https://ids.xmu.edu.cn/authserver/login?service=https://xmuxg.xmu.edu.cn/login/cas/xmu', [
            RequestOptions::HEADERS => [
                'origin' => 'https://ids.xmu.edu.cn',
                'referer' => 'https://ids.xmu.edu.cn/authserver/login?service=https://xmuxg.xmu.edu.cn/login/cas/xmu',
            ],
            RequestOptions::FORM_PARAMS => $form,
        ]);

        if ($this->authed()) {
            if (!$this->standalone) {
                if (is_null($this->user->id)) {
                    $this->user->id = Str::orderedUuid()->toString();
                }

                $this->user->cookie = json_encode($this->cookies->toArray());
                $this->user->save();
            }

            return true;
        }

        return false;
    }

    /**
     * Logout.
     *
     * @return bool true if success, otherwise false
     * @throws GuzzleException exception
     */
    public function logout(): bool
    {
        if ($this->authed()) {
            $this->client->get('https://ids.xmu.edu.cn/authserver/logout?service=https://xmuxg.xmu.edu.cn/xmu/login', [
                RequestOptions::ALLOW_REDIRECTS => false,
                RequestOptions::HEADERS => [
                    'referer' => 'https://xmuxg.xmu.edu.cn/',
                ],
            ]);
        }

        return !$this->authed();
    }

    /**
     * Clock.
     *
     * @return bool true if success, otherwise false
     * @throws GuzzleException exception
     */
    public function clock(): bool
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
            return false;
        }

        $deadline = $ownerNode->endDate ?? $business->endTime;

        if ($deadline == null || Carbon::parse($deadline)->isPast()) {
            return false;
        }

        $instance = json_decode($this->client->get("https://xmuxg.xmu.edu.cn/api/formEngine/business/{$business->id}/myFormInstance",
            [
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
        ]);

        // todo: validate

        return true;
    }

    /**
     * Auth check.
     *
     * @return bool true if authed, otherwise false
     * @throws GuzzleException exception
     */
    public function authed(): bool
    {
        return $this->client->get('https://xmuxg.xmu.edu.cn/login/check', [
                RequestOptions::ALLOW_REDIRECTS => false,
                RequestOptions::HEADERS => [
                    'referer' => 'https://xmuxg.xmu.edu.cn/platform',
                ],
            ])->getStatusCode() === 200;
    }
}
