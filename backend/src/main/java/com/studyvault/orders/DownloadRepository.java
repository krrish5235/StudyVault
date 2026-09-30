package com.studyvault.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DownloadRepository extends JpaRepository<Download, UUID> {
}
