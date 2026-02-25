export type Subcategory = {
  id: number;
  name: string;
  description?: string;
  defaultAssigneeName?: string;
};

export type Category = {
  id: number;
  name: string;
  description?: string;
  defaultAssigneeName?: string;
  subcategories: Subcategory[];
};
