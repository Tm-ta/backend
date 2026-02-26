variable "aws_region" {
  description = "AWS region for all resources (ALB/EC2/ACM/Route53 record)."
  type        = string
  default     = "ap-northeast-2"
}

variable "name_prefix" {
  description = "Prefix used in resource names and tags."
  type        = string
  default     = "tmta-api"
}

variable "api_domain_name" {
  description = "FQDN for the API endpoint."
  type        = string
  default     = "api.tm-ta.com"
}

variable "hosted_zone_id" {
  description = "Route53 public hosted zone ID that owns api_domain_name (e.g., tm-ta.com zone)."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the new VPC."
  type        = string
  default     = "10.50.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "Two public subnet CIDRs for ALB/EC2."
  type        = list(string)
  default     = ["10.50.1.0/24", "10.50.2.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) >= 2
    error_message = "At least two public subnets are required (ALB requires 2 AZs)."
  }
}

variable "allowed_ssh_cidrs" {
  description = "CIDRs allowed to SSH into the EC2 instance. Empty list disables SSH ingress."
  type        = list(string)
  default     = []
}

variable "auto_allow_current_public_ip_for_ssh" {
  description = "If true, Terraform fetches the current runner public IP and allows SSH from that /32."
  type        = bool
  default     = true
}

variable "allow_ec2_instance_connect" {
  description = "If true, allow SSH from AWS EC2 Instance Connect service IP ranges for the selected region (AWS Console browser SSH)."
  type        = bool
  default     = true
}

variable "key_name" {
  description = "Optional EC2 key pair name for SSH access."
  type        = string
  default     = null
}

variable "instance_type" {
  description = "EC2 instance type for the API server."
  type        = string
  default     = "m7i-flex.large"
}

variable "app_port" {
  description = "Port exposed by the API service on EC2 and used by the ALB target group."
  type        = number
  default     = 8080
}

variable "management_port" {
  description = "Spring Boot management/actuator port used for internal health checks (ALB -> EC2 only)."
  type        = number
  default     = 8081
}

variable "health_check_path" {
  description = "ALB health check path. Recommended: Spring Boot readiness endpoint."
  type        = string
  default     = "/actuator/health/readiness"
}

variable "root_volume_size_gb" {
  description = "Root EBS volume size (GiB)."
  type        = number
  default     = 30
}

variable "swap_size_gb" {
  description = "Swap file size (GiB) to create on the EC2 instance for memory pressure protection."
  type        = number
  default     = 4
}

variable "tags" {
  description = "Additional tags applied to resources."
  type        = map(string)
  default     = {}
}
