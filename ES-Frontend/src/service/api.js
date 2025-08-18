// src/utils/api.ts
import axios from "axios";
export const verifyAccount = (email, code) => {
    return axios.post("/api/auth/verify", {
        email: email,
        verificationCode: code,
    });
};
