"""AllNewsAPI exception classes."""


class NewsAPIError(Exception):
    """Exception raised for AllNewsAPI errors.

    Attributes:
        status_code: The HTTP status code of the error response.
        message: A human-readable error description.
    """

    def __init__(self, status_code: int, message: str) -> None:
        self.status_code = status_code
        self.message = message
        super().__init__(f"NewsAPI Error ({status_code}): {message}")
