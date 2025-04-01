// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

namespace Shpurdp.SCOM.ScomPages.DiscoveryTemplate
{
    partial class ShpurdpDetailsPage {
        /// <summary> 
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary> 
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing) {
            if (disposing && (components != null)) {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Component Designer generated code

        /// <summary> 
        /// Required method for Designer support - do not modify 
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent() {
            this.lblDescription = new System.Windows.Forms.Label();
            this.lblTitle = new Microsoft.EnterpriseManagement.Mom.Internal.UI.Controls.PageSectionLabel();
            this.cbRunAsAccount = new System.Windows.Forms.ComboBox();
            this.lblRunAsAccount = new System.Windows.Forms.Label();
            this.txtShpurdpUri = new System.Windows.Forms.TextBox();
            this.lblShpurdpUri = new System.Windows.Forms.Label();
            this.SuspendLayout();
            // 
            // lblDescription
            // 
            this.lblDescription.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.lblDescription.ImeMode = System.Windows.Forms.ImeMode.NoControl;
            this.lblDescription.Location = new System.Drawing.Point(0, 39);
            this.lblDescription.Name = "lblDescription";
            this.lblDescription.Size = new System.Drawing.Size(436, 88);
            this.lblDescription.TabIndex = 12;
            this.lblDescription.Text = "Provide details about the Hadoop subscriptions you want to monitor.\r" +
    "\n";
            // 
            // lblTitle
            // 
            this.lblTitle.BackColor = System.Drawing.Color.Transparent;
            this.lblTitle.Font = new System.Drawing.Font("Tahoma", 8.25F, System.Drawing.FontStyle.Bold);
            this.lblTitle.ImeMode = System.Windows.Forms.ImeMode.NoControl;
            this.lblTitle.Location = new System.Drawing.Point(0, 10);
            this.lblTitle.MinimumSize = new System.Drawing.Size(244, 0);
            this.lblTitle.Name = "lblTitle";
            this.lblTitle.Size = new System.Drawing.Size(270, 18);
            this.lblTitle.TabIndex = 7;
            this.lblTitle.Text = "Hadoop Details";
            // 
            // cbRunAsAccount
            // 
            this.cbRunAsAccount.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.cbRunAsAccount.DropDownStyle = System.Windows.Forms.ComboBoxStyle.DropDownList;
            this.cbRunAsAccount.FormattingEnabled = true;
            this.cbRunAsAccount.Location = new System.Drawing.Point(3, 210);
            this.cbRunAsAccount.Name = "cbRunAsAccount";
            this.cbRunAsAccount.Size = new System.Drawing.Size(433, 21);
            this.cbRunAsAccount.TabIndex = 10;
            this.cbRunAsAccount.SelectedIndexChanged += new System.EventHandler(this.ValidatePageConfigurationEventHandler);
            // 
            // lblRunAsAccount
            // 
            this.lblRunAsAccount.AutoSize = true;
            this.lblRunAsAccount.ImeMode = System.Windows.Forms.ImeMode.NoControl;
            this.lblRunAsAccount.Location = new System.Drawing.Point(0, 194);
            this.lblRunAsAccount.Name = "lblRunAsAccount";
            this.lblRunAsAccount.Size = new System.Drawing.Size(143, 13);
            this.lblRunAsAccount.TabIndex = 11;
            this.lblRunAsAccount.Text = "Credentials Run As Account:";
            // 
            // txtShpurdpUri
            // 
            this.txtShpurdpUri.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.txtShpurdpUri.Location = new System.Drawing.Point(3, 158);
            this.txtShpurdpUri.Name = "txtShpurdpUri";
            this.txtShpurdpUri.Size = new System.Drawing.Size(433, 20);
            this.txtShpurdpUri.TabIndex = 9;
            this.txtShpurdpUri.TextChanged += new System.EventHandler(this.ValidatePageConfigurationEventHandler);
            // 
            // lblShpurdpUri
            // 
            this.lblShpurdpUri.AutoSize = true;
            this.lblShpurdpUri.ImeMode = System.Windows.Forms.ImeMode.NoControl;
            this.lblShpurdpUri.Location = new System.Drawing.Point(0, 142);
            this.lblShpurdpUri.Name = "lblShpurdpUri";
            this.lblShpurdpUri.Size = new System.Drawing.Size(64, 13);
            this.lblShpurdpUri.TabIndex = 8;
            this.lblShpurdpUri.Text = "Shpurdp URI:";
            // 
            // ShpurdpDetailsPage
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.Controls.Add(this.lblDescription);
            this.Controls.Add(this.lblTitle);
            this.Controls.Add(this.cbRunAsAccount);
            this.Controls.Add(this.lblRunAsAccount);
            this.Controls.Add(this.txtShpurdpUri);
            this.Controls.Add(this.lblShpurdpUri);
            this.HeaderText = "Hadoop Details";
            this.Name = "ShpurdpDetailsPage";
            this.NavigationText = "Hadoop Details";
            this.Size = new System.Drawing.Size(456, 400);
            this.TabName = "Hadoop Details";
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Label lblDescription;
        private Microsoft.EnterpriseManagement.Mom.Internal.UI.Controls.PageSectionLabel lblTitle;
        private System.Windows.Forms.ComboBox cbRunAsAccount;
        private System.Windows.Forms.Label lblRunAsAccount;
        private System.Windows.Forms.TextBox txtShpurdpUri;
        private System.Windows.Forms.Label lblShpurdpUri;
    }
}
