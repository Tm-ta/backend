output "api_url" {
  description = "HTTPS URL for the API domain."
  value       = "https://${var.api_domain_name}"
}

output "alb_dns_name" {
  description = "ALB DNS name (useful for debugging before DNS propagates)."
  value       = aws_lb.api.dns_name
}

output "ec2_instance_id" {
  description = "EC2 instance ID for the API server."
  value       = aws_instance.api.id
}

output "ec2_public_ip" {
  description = "Public IP of the EC2 instance."
  value       = aws_instance.api.public_ip
}

output "ses_instance_role_arn" {
  description = "IAM role ARN attached to the EC2 instance for SES sending."
  value       = aws_iam_role.ec2_ses_sender.arn
}

output "s3_bucket_name" {
  description = "S3 bucket used for profile image uploads."
  value       = aws_s3_bucket.assets.bucket
}

output "acm_certificate_arn" {
  description = "ACM certificate ARN used by the HTTPS listener."
  value       = aws_acm_certificate_validation.api.certificate_arn
}

output "effective_ssh_cidrs" {
  description = "Effective SSH allowlist CIDRs applied to the EC2 security group."
  value       = local.effective_ssh_cidrs
}
