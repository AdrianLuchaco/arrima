/** An error answered by the backend (RFC 9457 body with a "code"), or a network failure. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fields: Record<string, string>
  readonly details: Record<string, unknown>

  constructor(status: number, code: string, fields: Record<string, string> = {}, details: Record<string, unknown> = {}) {
    super(code)
    this.status = status
    this.code = code
    this.fields = fields
    this.details = details
  }

  get isNetworkError() {
    return this.status === 0
  }

  static network() {
    return new ApiError(0, 'NETWORK')
  }

  static async from(response: Response): Promise<ApiError> {
    try {
      const body = (await response.json()) as Record<string, unknown>
      const { code, fields, ...details } = body
      return new ApiError(
        response.status,
        typeof code === 'string' ? code : 'UNKNOWN',
        (fields as Record<string, string>) ?? {},
        details,
      )
    } catch {
      return new ApiError(response.status, response.status >= 500 ? 'SERVER' : 'UNKNOWN')
    }
  }
}
