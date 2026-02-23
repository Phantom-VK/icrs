export function getErrorMessage(error: any, fallback: string): string {
  if (error?.response?.data) {
    const data = error.response.data;
    if (typeof data === "string") return data;
    if (typeof data.message === "string") return data.message;
    if (data.error) return String(data.error);
  }
  if (error?.message) return String(error.message);
  return fallback;
}
