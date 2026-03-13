export type GrievanceWithDates = {
  status: string;
  createdAt?: string;
  updatedAt?: string;
};

const getSortableTimestamp = (item: Pick<GrievanceWithDates, "createdAt" | "updatedAt">): number => {
  if (item.createdAt) {
    return new Date(item.createdAt).getTime();
  }

  if (item.updatedAt) {
    return new Date(item.updatedAt).getTime();
  }

  return 0;
};

export const sortGrievancesByLatest = <T extends Pick<GrievanceWithDates, "createdAt" | "updatedAt">>(
  items: T[]
): T[] => {
  return [...items].sort((a, b) => getSortableTimestamp(b) - getSortableTimestamp(a));
};

export const getActiveSortedGrievances = <T extends GrievanceWithDates>(items: T[]): T[] => {
  return items
    .filter(
      (g) => g.status === "SUBMITTED" || g.status === "IN_PROGRESS"
    )
    .sort((a, b) => getSortableTimestamp(b) - getSortableTimestamp(a));
};

export const getHistorySortedGrievances = <T extends GrievanceWithDates>(items: T[]): T[] => {
  return items
    .filter((g) => g.status !== "SUBMITTED" && g.status !== "IN_PROGRESS")
    .sort((a, b) => getSortableTimestamp(b) - getSortableTimestamp(a));
};
