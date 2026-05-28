import type { AdmissionResponse } from '../api/types';

export type AdmissionStayOutletContext = {
  admission: AdmissionResponse | null;
  patientName: string;
};
