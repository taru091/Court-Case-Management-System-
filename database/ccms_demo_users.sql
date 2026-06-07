USE ccms_db;

-- Demo credentials:
-- admin / admin
-- lawyer / lawyer123
-- staff / staff123
-- judge / judge123
-- citizen / citizen123
--
-- Passwords below are SHA-256 hashes of the plaintext credentials above.
INSERT INTO users (
    username,
    name,
    mobile,
    email,
    password,
    role,
    occupation,
    bar_council_number,
    court_id,
    aadhaar_number,
    profile_photo_url,
    approval_status,
    availability_status
) VALUES
('admin', 'Admin', '9999999991', 'admin@ccms.com', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'Admin', 'Admin', NULL, NULL, '111122223333', NULL, 'Approved', 'Available'),
('lawyer', 'Lawyer', '9999999992', 'lawyer@ccms.com', 'ac3226b60081e5f9f9f1f784838aca038eb7c2f7411cb90702b6c2bfe07a45a9', 'Lawyer', 'Lawyer', 'BCI-LAW-1001', NULL, '222233334444', NULL, 'Approved', 'Available'),
('staff', 'Staff', '9999999993', 'staff@ccms.com', '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6', 'Staff', 'Staff', NULL, NULL, '333344445555', NULL, 'Approved', 'Available'),
('judge', 'Judge', '9999999994', 'judge@ccms.com', '94358d5abe1d055c1ced4403bb0e397edf8c905b33e03b34ad1b1d3adf2d9cf4', 'Judge', 'Judge', NULL, 'COURT-101', '444455556666', NULL, 'Approved', 'Available'),
('citizen', 'Citizen', '9999999995', 'citizen@ccms.com', '4b4b4c19fdc4b422ca5a52085c3ba8fd2087c62afb06dae791f8fb9c51c56b4b', 'Citizen', 'Citizen', NULL, NULL, '555566667777', NULL, 'Approved', 'Available')
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    name = VALUES(name),
    mobile = VALUES(mobile),
    email = VALUES(email),
    password = VALUES(password),
    role = VALUES(role),
    occupation = VALUES(occupation),
    bar_council_number = VALUES(bar_council_number),
    court_id = VALUES(court_id),
    aadhaar_number = VALUES(aadhaar_number),
    profile_photo_url = VALUES(profile_photo_url),
    approval_status = VALUES(approval_status),
    availability_status = VALUES(availability_status);
