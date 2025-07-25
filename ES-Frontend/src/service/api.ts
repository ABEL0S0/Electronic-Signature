// src/utils/api.ts
import axios from "axios";

export const verifyAccount = (email: string, code: string) => {
  return axios.post("/api/auth/verify", {
    email: email,
    verificationCode: code,
  });
};
