package allnewsapi

import "fmt"

// APIError represents an error returned by the AllNewsAPI.
type APIError struct {
	StatusCode int
	Message    string
}

// Error implements the error interface.
func (e *APIError) Error() string {
	return fmt.Sprintf("AllNewsAPI Error (%d): %s", e.StatusCode, e.Message)
}
