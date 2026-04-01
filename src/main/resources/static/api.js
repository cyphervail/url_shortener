/**
 * api.js — network layer for snip.
 *
 * Only talks to the backend. No DOM, no UI state.
 * Throws ApiError on failure — app.js decides what the user sees.
 *
 * Endpoints:
 *   POST /shortUrl  →  { longUrl, expiredAt? }  →  { shortUrl }
 */

const API_BASE = 'http://localhost:8080';

/**
 * @param {string} longUrl
 * @param {string|null} expiredAt  - "yyyy-MM-ddTHH:mm" or null
 * @returns {Promise<{ shortUrl: string }>}
 * @throws {ApiError}
 */
async function createShortUrl(longUrl, expiredAt) {
  const body = { longUrl };
  if (expiredAt) body.expiredAt = expiredAt;

  let response;

  try {
    response = await fetch(API_BASE + '/shortUrl', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0);
  }

  if (!response.ok) {
    throw new ApiError(response.status);
  }

  return response.json();
}

/**
 * Thin error carrier — only the HTTP status crosses the boundary.
 * app.js owns all user-facing copy.
 */
class ApiError extends Error {
  constructor(status) {
    super('ApiError(' + status + ')');
    this.name   = 'ApiError';
    this.status = status;
  }
}
