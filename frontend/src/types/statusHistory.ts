export type StatusHistory = {
  id?: number;
  fromStatus?: string;
  toStatus: string;
  actorName?: string;
  changedAt?: string;
  reason?: string;
};
