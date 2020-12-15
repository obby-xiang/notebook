<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class HealthClock extends Model
{
    use HasFactory;

    protected $keyType = 'uuid';

    public $incrementing = false;

    protected $fillable = [
        'id',
        'user_id',
        'status',
        'excepted_clocked_at',
        'clocked_at',
        'message',
    ];

    protected $casts = [
        'excepted_clocked_at' => 'datetime',
        'clocked_at' => 'datetime',
    ];

    /**
     * User.
     *
     * @return BelongsTo belongs to
     */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class, 'user_id', 'id');
    }
}
