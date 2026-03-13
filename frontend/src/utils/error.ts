export function getErrorMessage(error: any, fallback: string): string {
  if (error?.response?.data) {
    const data = error.response.data;
    if (typeof data === "string") return data;
    if (typeof data.message === "string") return data.message;
    if (typeof data.email === "string" && !data.token) return data.email;
    if (data.error) return String(data.error);
  }
  if (error?.message) return String(error.message);
  return fallback;
}

export function getCommonAuthErrorMessage(message: string): string {
  const normalized = message.toLowerCase();

  if (
    normalized.includes("bad credentials") ||
    normalized.includes("password does not match") ||
    normalized.includes("invalid credentials")
  ) {
    return "Incorrect email or password. Check your credentials and try again.";
  }

  if (normalized.includes("account not verified")) {
    return "Your account is not verified yet. Complete email verification before signing in.";
  }

  if (normalized.includes("user not found")) {
    return "No account was found for this email address.";
  }

  if (
    normalized.includes("network error") ||
    normalized.includes("failed to fetch") ||
    normalized.includes("request failed")
  ) {
    return "The server could not be reached. Check the backend and try again.";
  }

  return message;
}
