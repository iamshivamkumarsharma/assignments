export interface Scan {
  id?: number | null;
  domainName: string;
  numPages: number;
  numBrokenLinks: number;
  numMissingImages: number;
  deleted?: boolean;
}

/** Columns the backend allows for the search `orderBy` parameter. */
export const SCAN_ORDER_BY_COLUMNS = [
  'id',
  'domainName',
  'numPages',
  'numBrokenLinks',
  'numMissingImages',
  'deleted',
] as const;

export type ScanOrderByColumn = (typeof SCAN_ORDER_BY_COLUMNS)[number];
