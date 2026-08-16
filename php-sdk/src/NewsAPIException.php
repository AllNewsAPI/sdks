<?php

namespace AllNewsAPI;

class NewsAPIException extends \Exception
{
    private int $statusCode;

    public function __construct(string $message, int $statusCode = 0)
    {
        parent::__construct($message, $statusCode);
        $this->statusCode = $statusCode;
    }

    public function getStatusCode(): int
    {
        return $this->statusCode;
    }
}
