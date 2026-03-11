import type { ReactNode } from "react";

export interface WholeSyncTaskDraft {
    source: {
        dbType: string;
        connectorType: string;
        datasourceId: string;
        pluginName: string;
        extraParams: any[];
        startupMode: any;
        stopMode: any;
        schemaChange: boolean
    };
    target: {
        dbType: string;
        connectorType: string;
        datasourceId: string;
        pluginName: string;
        extraParams: any[];
        dataSaveMode: any;
        batchSize: any;
        exactlyOnce: boolean;
        schemaSaveMode: any;
        enableUpsert: boolean;
    };
    tableMatch: {
        mode: "1" | "2" | "3" | "4";
        keyword?: string;
        tables?: string[];
    };
}



export interface TableItem {
    key: string;
    title: ReactNode;
    rawTitle: string;
    chosen?: boolean;
}

export interface DbType {
    dbType: string;
    connectorType: string;
    pluginName: string;
}