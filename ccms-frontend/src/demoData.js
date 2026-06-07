import { matchesSearch } from "./api";

export const demoCases = [
  {
    caseId: 101,
    caseName: "State vs Turner Holdings",
    clientName: "Avery Turner",
    lawyerName: "Lawyer",
    lawyerUserId: 2,
    judgeName: "Judge",
    judgeUserId: 4,
    status: "Active",
    courtDetails: "District Court Hall A",
    createdAt: "2026-04-10 10:30",
    nextHearingDate: "2026-05-12 11:00",
    nextCourtroom: "Courtroom A"
  },
  {
    caseId: 102,
    caseName: "Riverside Property Appeal",
    clientName: "Riverside Group",
    lawyerName: "Lawyer",
    lawyerUserId: 2,
    judgeName: "Judge",
    judgeUserId: 4,
    status: "Pending",
    courtDetails: "District Court Hall B",
    createdAt: "2026-04-18 14:15",
    nextHearingDate: "2026-05-20 09:30",
    nextCourtroom: "Courtroom C"
  },
  {
    caseId: 103,
    caseName: "Maya Foods Contract Review",
    clientName: "Maya Foods Ltd.",
    lawyerName: "Lawyer",
    lawyerUserId: 2,
    judgeName: "Judge",
    judgeUserId: 4,
    status: "Closed",
    courtDetails: "District Court Hall C",
    createdAt: "2026-03-28 12:00",
    nextHearingDate: null,
    nextCourtroom: null
  },
  {
    caseId: 104,
    caseName: "City Works Compliance Matter",
    clientName: "City Works",
    lawyerName: "Lawyer",
    lawyerUserId: 2,
    judgeName: "Judge",
    judgeUserId: 4,
    status: "Active",
    courtDetails: "District Court Hall A",
    createdAt: "2026-04-25 16:10",
    nextHearingDate: "2026-05-09 14:00",
    nextCourtroom: "Courtroom A"
  },
  {
    caseId: 105,
    caseName: "Northwind Evidence Review",
    clientName: "Northwind Services",
    lawyerName: "Lawyer",
    lawyerUserId: 2,
    judgeName: "Judge",
    judgeUserId: 4,
    status: "Pending",
    courtDetails: "District Court Hall E",
    createdAt: "2026-04-30 09:20",
    nextHearingDate: "2026-05-15 10:45",
    nextCourtroom: "Courtroom E"
  },
  {
    caseId: 106,
    caseName: "Horizon Estate Settlement",
    clientName: "Laura Benton",
    lawyerName: "Lawyer",
    lawyerUserId: 2,
    judgeName: "Judge",
    judgeUserId: 4,
    status: "Active",
    courtDetails: "District Court Hall F",
    createdAt: "2026-05-01 08:50",
    nextHearingDate: "2026-05-18 13:15",
    nextCourtroom: "Courtroom F"
  }
];

export const demoDashboard = {
  totalCases: 6,
  activeCases: 3,
  pendingCases: 2,
  closedCases: 1,
  upcomingHearings: 5
};

export const demoStatusAnalytics = {
  Active: 3,
  Pending: 2,
  Closed: 1
};

export const demoDelayAnalytics = {
  delayedCases: 1,
  averageDelayDays: 4.5
};

export const demoJudgeLoad = [
  { judgeName: "Hon. Eleanor Blake", totalCases: 2 },
  { judgeName: "Hon. Samuel Reed", totalCases: 2 },
  { judgeName: "Hon. David Ortiz", totalCases: 1 },
  { judgeName: "Hon. Nina Clarke", totalCases: 1 }
];

export const demoHearingsByCase = {
  101: [
    { hearingId: 1, caseId: 101, hearingDate: "2026-05-12 11:00", courtroom: "Courtroom A" },
    { hearingId: 2, caseId: 101, hearingDate: "2026-05-28 10:00", courtroom: "Courtroom B" }
  ],
  102: [
    { hearingId: 3, caseId: 102, hearingDate: "2026-05-20 09:30", courtroom: "Courtroom C" }
  ],
  103: [
    { hearingId: 4, caseId: 103, hearingDate: "2026-04-04 12:00", courtroom: "Courtroom D" }
  ],
  104: [
    { hearingId: 5, caseId: 104, hearingDate: "2026-05-09 14:00", courtroom: "Courtroom A" }
  ],
  105: [
    { hearingId: 6, caseId: 105, hearingDate: "2026-05-15 10:45", courtroom: "Courtroom E" }
  ],
  106: [
    { hearingId: 7, caseId: 106, hearingDate: "2026-05-18 13:15", courtroom: "Courtroom F" }
  ]
};

export const demoDocumentsByCase = {
  101: [
    { documentId: 1, caseId: 101, fileName: "notice.pdf", fileUrl: "C:/ccms-documents/turner/notice.pdf", approvalStatus: "APPROVED" },
    { documentId: 2, caseId: 101, fileName: "evidence-list.pdf", fileUrl: "C:/ccms-documents/turner/evidence-list.pdf", approvalStatus: "APPROVED" }
  ],
  102: [
    { documentId: 3, caseId: 102, fileName: "appeal-summary.docx", fileUrl: "C:/ccms-documents/riverside/appeal-summary.docx", approvalStatus: "APPROVED" }
  ],
  104: [
    { documentId: 4, caseId: 104, fileName: "compliance-report.pdf", fileUrl: "C:/ccms-documents/cityworks/compliance-report.pdf", approvalStatus: "APPROVED" }
  ]
};

export const demoDocuments = Object.values(demoDocumentsByCase).flat();

export function getFilteredDemoCases(query) {
  return demoCases.filter((caseItem) => matchesSearch(caseItem, query));
}
