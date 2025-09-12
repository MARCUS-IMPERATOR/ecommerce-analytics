import { useState } from "react";
import { apiClient } from "../services/api";
import {IMAGES} from "../assets/image.ts"

const Login = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    const handleSubmit = async () => {
        console.log('Login attempt:', { username, password });
        
        try{
            const result = await apiClient.post<{token:string}>("/login",{username,password});
            const token = typeof result === "string" ? result : result.token;

            if (!token) throw new Error("No token returned from server");

            localStorage.setItem("token", token)
            console.log("Login successful");
        }
        catch(err:unknown){
            console.error("Login error", err);
        }
    };

    return (
        <>
            <style>{`

            html, body {
                height: 100%;
                margin: 0;
                font-family: 'Roboto', sans-serif;
            }

            .container {
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 16px;
                position: relative;
                overflow: hidden;
                height: 100vh;
                background-image: url(${IMAGES.Background});
                background-size: cover;
                background-position: center;
            }

            .container::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background-color: rgba(0, 0, 0, 0.64);
                z-index: 1;
            }

            .login-card {
                background-color: white;
                border-radius: 24px;
                box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
                padding: 32px;
                width: 100%;
                max-width: 448px;
                position: relative;
                z-index: 10;
            }

            .header {
                text-align: center;
                margin-bottom: 32px;
            }

            .logo-container {
                display: flex;
                align-items: center;
                justify-content: center;
                margin-bottom: 16px;
            }

            .logo-img {
                width: 150px;
            }

            .title {
                font-size: 30px;
                font-weight: bold;
                color: #374151;
                line-height: 1.2;
                margin: 0;
            }

            .form-container {
                display: flex;
                flex-direction: column;
                gap: 24px;
            }

            .input-group {
                display: flex;
                flex-direction: column;
            }

            .input-label {
                display: block;
                font-size: 14px;
                font-weight: 500;
                color: #374151;
                margin-bottom: 8px;
            }

            .input-field {
                width: 100%;
                padding: 12px 16px;
                border: 1px solid #e5e7eb;
                border-radius: 8px;
                outline: none;
                transition: all 0.2s ease;
                color: #6b7280;
                font-size: 16px;
                box-sizing: border-box;
            }

            .input-field::placeholder {
                color: #9ca3af;
            }

            .input-field:focus {
                border-color: transparent;
                box-shadow: 0 0 0 2px #3b82f6;
            }

            .login-button {
                width: 100%;
                background-color: #3b82f6;
                color: white;
                font-weight: 500;
                padding: 12px 16px;
                border: none;
                border-radius: 8px;
                cursor: pointer;
                transition: all 0.2s ease;
                font-size: 16px;
                outline: none;
            }

            .login-button:hover {
                background-color: #2563eb;
            }

            .login-button:focus {
                box-shadow: 0 0 0 2px #3b82f6, 0 0 0 4px rgba(59, 130, 246, 0.1);
            }
        `}</style>

            <div className="container">
                <div className="login-card">
                    <div className="header">
                        <div className="logo-container">
                            <img src= {IMAGES.Logo} alt="App Logo" className="logo-img" />
                        </div>
                        <h1 className="title">
                            Login
                        </h1>
                    </div>

                    <div className="form-container">
                        <div className="input-group">
                            <div className="input-label">
                                Username
                            </div>
                            <input
                                type="text"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                placeholder="Text"
                                className="input-field"
                            />
                        </div>

                        <div className="input-group">
                            <div className="input-label">
                                Password
                            </div>
                            <input
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Text"
                                className="input-field"
                            />
                        </div>

                        <button
                            onClick={handleSubmit}
                            className="login-button"
                        >
                            Login
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
};

export default Login;
