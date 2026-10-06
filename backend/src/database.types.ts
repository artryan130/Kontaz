export type TransactionRow = {
  id: string;
  user_id: string;
  amount: number;
  type: "income" | "expense" | "investment";
  category: string;
  description: string | null;
  date: string;
  created_at: string;
  is_recurring: boolean | null;
  end_date: string | null;
  is_installment: boolean | null;
  installment_count: number | null;
  installment_number: number | null;
  payment_method: string | null;
}

interface Table<Row, Insert, Update> {
  Row: Row;
  Insert: Insert;
  Update: Update;
  Relationships: [];
}

export interface Database {
  public: {
    Tables: {
      transactions: Table<
        TransactionRow,
        Omit<
          TransactionRow,
          | "id"
          | "created_at"
          | "description"
          | "is_recurring"
          | "end_date"
          | "is_installment"
          | "installment_count"
          | "installment_number"
          | "payment_method"
        > &
          Partial<
            Pick<
              TransactionRow,
              | "description"
              | "is_recurring"
              | "end_date"
              | "is_installment"
              | "installment_count"
              | "installment_number"
              | "payment_method"
            >
          > & { id?: string; created_at?: string },
        Partial<Omit<TransactionRow, "id" | "user_id" | "created_at">>
      >;
      profiles: Table<
        { id: string; full_name: string | null; avatar_url: string | null; subscription_plan: string | null },
        { id: string; full_name?: string | null; avatar_url?: string | null; subscription_plan?: string | null },
        { full_name?: string | null; avatar_url?: string | null; subscription_plan?: string | null }
      >;
      settings: Table<
        { id: string; user_id: string; safety_percentage: number },
        { id?: string; user_id: string; safety_percentage?: number },
        { safety_percentage?: number }
      >;
    };
    Views: Record<string, never>;
    Functions: Record<string, never>;
    Enums: Record<string, never>;
    CompositeTypes: Record<string, never>;
  };
}
