export type GrievanceWithDates = {
  status: string;
  createdAt?: string;
  updatedAt?: string;
};

export const getActiveSortedGrievances = <T extends GrievanceWithDates>(items: T[]): T[] => {
  return items
    .filter(
      (g) => g.status === "SUBMITTED" || g.status === "IN_PROGRESS"
    )
    .sort((a, b) => {
      const aTime = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const bTime = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return bTime - aTime;
    });
};

export const getHistorySortedGrievances = <T extends GrievanceWithDates>(items: T[]): T[] => {
  return items
    .filter((g) => g.status !== "SUBMITTED" && g.status !== "IN_PROGRESS")
    .sort((a, b) => {
      const aTime = a.updatedAt
        ? new Date(a.updatedAt).getTime()
        : a.createdAt
          ? new Date(a.createdAt).getTime()
          : 0;
      const bTime = b.updatedAt
        ? new Date(b.updatedAt).getTime()
        : b.createdAt
          ? new Date(b.createdAt).getTime()
          : 0;
      return bTime - aTime;
    });
};
